/*
    This job serves to watch https://github.com/edx/configuration for changes pushed to a particular branch. Upon a push to that branch, the job is
    triggered via a webhook, at which point it runs the parsefiles.py script with the files changed between the old commit and the new commit. 
    This resolves which Docker images need to be rebuilt as a result of the aforementioned changes to the configuration repository. For each 
    Docker image that needs rebuilding, this job triggers the respective <app>-watcher downstream job, which then calls the image-builder job 
    to build and push up the respective image to DockerHub.

    Branches other than master should be prefixed with "refs/heads/" for the Jenkins Git plugin to match them
    correctly when a webhook is received.  For example, "refs/heads/open-release/hawthorn.master".

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    APPS_TO_CONFIG: a dictionary containing mappings from an edX IDA to a dictionary of various configuration values (REQUIRED). 
        The structure of APPS_TO_CONFIG should be
        
        APPS_TO_CONFIG:
            <IDA name>:
                app_repo: <GitHub repository name for the IDA>
                app_repo_branch: <branch of the above repository to checkout>
                config_branch: <GitHub branch of configuration repository to checkout>
                tag_name: <tag to use for the built image>
        For example, 
        APPS_TO_CONFIG:
            ecommerce:
                app_repo: 'ecommerce'
                app_repo_branch: 'master'
                config_branch: 'master'
                tag_name: 'latest'
        Note that the default for app_repo_branch is master, the default for config_branch is master, and the default for tag_name is latest;
        therefore, they do not need to be specified unless you are providing overrides.
        
        For example,
        APPS_TO_CONFIG:
            ecommerce:
                app_repo: 'ecommerce'
                
        accomplishes the same as the dictionary above.
    CONFIGURATION_REPO_URL: URL of the configuration GitHub repository (REQUIRED).
    CONFIGURATION_BRANCH: the branch of the configuration repository to watch for changes
    FOLDER_NAME: the name of the folder in which the downstream per-app jobs will live; defaults to DockerCI.
    ACCESS_CONTROL: list of github users or groups who will have access to the jobs.
    
    Expected credentials - these will normally be set up on the Folder.
    docker-config-json: a file credential that gives each job in the folder permission to push to DockerHub.
*/

package devops.jobs

import static org.edx.jenkins.dsl.YAMLHelpers.parseYaml
import org.yaml.snakeyaml.error.YAMLException

import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class ConfigurationWatcher {
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME", "DockerCI") + "/configuration-watcher") {

            assert extraVars.containsKey('CONFIGURATION_REPO_URL') : "Please define CONFIGURATION_REPO_URL"
            assert extraVars.containsKey('APPS_TO_CONFIG'): "Please define APPS_TO_CONFIG"

            def config_branch = extraVars.get('CONFIGURATION_BRANCH', 'master')

            // to space separated list as string
            def apps = extraVars.get("APPS_TO_CONFIG").keySet().join(" ")

            // inject APPS as an environment variable to be used by trigger-builds.sh
            environmentVariables {
                env('APPS', apps)
                env('CI_PYTHON_VERSION', extraVars.get('CI_PYTHON_VERSION'))
            }

            description('\rThis job watches the configuration repository for changes via a webhook. Upon a change, the job runs a script ' + 
                'that determines what images must be rebuilt as a result of the change to configuration. It then triggers the associated ' +
                'image-builder jobs.')

            wrappers common_wrappers

            logRotator common_logrotator

            def access_control = extraVars.get('ACCESS_CONTROL',[])
            access_control.each { acl ->
                common_read_permissions.each { perm ->
                    authorization {
                        permission(perm,acl)
                    }
                }
            }

            // for GitHub Webhooks; note that you need to use an https URL and include
            // the trailing slash
            properties {
                githubProjectUrl(extraVars.get("CONFIGURATION_REPO_URL"))
            }

            scm {
                // check out edx/configuration repository from GitHub
                git {
                    remote {
                        url(extraVars.get("CONFIGURATION_REPO_URL"))
                        branch(config_branch)
                    }
                    extensions {
                        relativeTargetDirectory('configuration')
                    }
                }
            }
            
            triggers { githubPush() }

            // run the trigger-builds shell script in a virtual environment called venv
            steps {
                shell(dslFactory.readFileFromWorkspace('devops/resources/trigger-builds.sh'))

                // inject environment variables defined in the temp_props file (TO_BUILD)
                // temp_props is a file that is created from devops/resources/trigger-builds.sh,
                // which contains the value of TO_BUILD
                environmentVariables {
                    propertiesFile('temp_props')
                }

                conditionalSteps {
                    // ensure TO_BUILD is not empty string to avoid failed build if nothing to be built
                    condition {
                        not {
                            stringsMatch('', '${TO_BUILD}', false)
                        }
                    }

                    steps {
                        // trigger the jobs defined in the TO_BUILD environment variable; this is set via the trigger-builds script
                        // and injected into the environment from the temp_props file
                        downstreamParameterized {
                            trigger('${TO_BUILD}') 
                        }
                    }
                } 
            }
        }
    }
}
