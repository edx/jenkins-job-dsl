/*
    This job handles changes to an application.

    For applications that have source code repositories, this job watches for changes pushed to a particular branch.
    Upon a push to that branch, the job is triggered via a webhook. The job triggers the matching downstream image-builder job at
    DockerCI/image-builders/<app>-image-builder. The <app>-image-builder then builds and pushes the application's Docker image to DockerHub.

    For applications that do not have source code repositories, there are no watcher jobs, because there are no repositories to watch. If a new
    image for these applications is needed, the associated <app>-image-builder job will need to be run manually.

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
        Note that the default for app_repo_branch is master, the default for config_branch is master, and the default
        for tag_name is latest; therefore, they do not need to be specified unless you are providing overrides.
        
        For example,
        APPS_TO_CONFIG:
            ecommerce:
                app_repo: 'ecommerce'
                
        accomplishes the same as the dictionary above.
    EDX_REPO_ROOT: the URL of the edX GitHub organization (REQUIRED).
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

class AppWatcher {
    public static def job = { dslFactory, extraVars ->

        //assertions come earlier to ensure APPS_TO_CONFIG can be read
        assert extraVars.containsKey('EDX_REPO_ROOT') : "Please define EDX_REPO_ROOT"
        assert extraVars.containsKey('APPS_TO_CONFIG'): "Please define APPS_TO_CONFIG"

        // for each application
        extraVars.get("APPS_TO_CONFIG").each { app_name, app_config ->

            def app_repo = app_config.get("app_repo", '')

            // we don't need a watcher job if there is no repository to watch
            if (app_repo) {
                dslFactory.job(extraVars.get("FOLDER_NAME", "DockerCI") + "/application-watchers/" + app_name + "-watcher") {

                    // if an application has no repository, the app_repo_branch should be blank so as not to set up webhooks
                    def app_repo_branch = app_config.get('app_repo_branch', 'master')

                    description('\rThis job watches the ' + app_repo_branch + ' branch of the ' + app_repo + ' repository for changes via a webhook. ' + 
                        'Upon a change to the branch, this job will run and trigger the downstream ' + app_name + '-image-builder job, which will ' +
                        'build and push the image to DockerHub. Applications without application code repositories do not have an associated watcher job.')

                    logRotator common_logrotator

                    wrappers common_wrappers

                    def access_control = extraVars.get('ACCESS_CONTROL',[])
                    access_control.each { acl ->
                        common_read_permissions.each { perm ->
                            authorization {
                                permission(perm,acl)
                            }
                        }
                    }

                    def app_repo_url = extraVars.get("EDX_REPO_ROOT") + app_repo + "/"
                    
                    // for GitHub Webhooks; note that you need to use an https URL and include
                    // the trailing slash
                    properties {
                       githubProjectUrl(app_repo_url)   
                    }

                    scm {
                        git {
                            remote {
                                url(app_repo_url)
                                branch(app_repo_branch)
                            }
                            extensions {
                                relativeTargetDirectory('edx')
                            }
                        }
                    }

// 2022-09-28 jdmulloy: Temporarily disable docker automatic builds due to server overload
//                    triggers { githubPush() }

                    steps {
                        // trigger image-builder job, passing commit checked out of the application code repository 
                       downstreamParameterized {
                            trigger('../image-builders/' + app_name + '-image-builder')
                        }
                    }
                }
            }
        }
    }
}
