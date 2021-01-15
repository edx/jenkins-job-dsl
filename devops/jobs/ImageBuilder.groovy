/*
    This job serves to build an application's Docker image and to push it to Docker Hub. This job only serves as a downstream job to the 
    various <app>-watcher jobs and does not run on its own. This job receives the name of an application as a build parameter. 
    This application's Docker image is then built and pushed to DockerHub.

    Branches other than master should be prefixed with "refs/heads/" for the Jenkins Git plugin to match them
    correctly when a webhook is received.  For example, "refs/heads/open-release/hawthorn.master".

    Variables are consumed from the EXTRA_VARS input to your seed job in addition
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
    
    CONFIGURATION_REPO_URL: URL of the configuration GitHub repository (REQUIRED)
    CONFIG_JSON_FILE_CREDENTIAL_ID: ID of the credentials set up in Jenkins to log into DockerHub; defaults to docker-config-json; this refers to the docker-config-json expected credential described below
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

class ImageBuilder {
    public static def job = { dslFactory, extraVars ->

        //assertions come earlier to ensure APPS_TO_CONFIG can be read
        assert extraVars.containsKey('APPS_TO_CONFIG'): "Please define APPS_TO_CONFIG"
        assert extraVars.containsKey('CONFIGURATION_REPO_URL') : "Please define CONFIGURATION_REPO_URL"

        // for each application
        extraVars.get("APPS_TO_CONFIG").each { app_name, app_config ->
            dslFactory.job(extraVars.get("FOLDER_NAME", "DockerCI") + "/image-builders/" + app_name + "-image-builder") {

                def app_repo_branch = app_config.get('app_repo_branch', 'master')
                def config_branch = app_config.get('config_branch', 'master')
                def tag_name = app_config.get('tag_name', 'latest')
                def openedx_release = app_repo_branch.replace('refs/heads/', '')

                // inject APP_NAME, OPENEDX_RELEASE, and TAG_NAME as environment variables for use by build-push-app.sh
                environmentVariables {
                    env("APP_NAME", app_name)
                    env('OPENEDX_RELEASE', openedx_release)
                    env('TAG_NAME', tag_name)
                    env('CI_PYTHON_VERSION', extraVars.get('CI_PYTHON_VERSION'))
                }

                description('\rThis job builds the ' + app_name + ' Docker image and pushes it to DockerHub. ' + 
                    'It is typically called from the upstream ' + app_name + '-watcher job or the configuration-watcher job, ' +
                    'but it can also be run manually to build and push the ' + app_name + ' Docker image. If the application has ' +
                    'no application code repository, it will only be triggered by changes picked up by the configuration-watcher job, or ' +
                    'it must be built by hand.')

                wrappers common_wrappers

                wrappers {
                    // add credentials to log in to DockerHub; ID refers to credential ID as set up in Jenkins
                    credentialsBinding {
                        file("CONFIG_JSON_FILE", extraVars.get("CONFIG_JSON_FILE_CREDENTIAL_ID", "docker-config-json"))
                    }
                    // Don't allow a stuck job to run indefinitely; longest success to date was around 35 minutes
                    timeout {
                        absolute(90)
                    }
                }

                logRotator common_logrotator

                def access_control = extraVars.get('ACCESS_CONTROL',[])
                access_control.each { acl ->
                    common_read_permissions.each { perm ->
                        authorization {
                            permission(perm,acl)
                        }
                    }
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

                triggers {
                    cron("H H * * H")
                }

                steps {
                    // run the build-push-app shell script in a virtual environment called venv
                    shell(dslFactory.readFileFromWorkspace('devops/resources/build-push-app.sh'))

                }
            }
        }
    }
}
