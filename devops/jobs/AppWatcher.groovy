/*
    This job handles changes to an application.

    For applications that have source code repositories, this job watches for changes pushed to master. 
    Upon a push to master, the job is triggered via a webhook. The job triggers the downstream image-builder job, passing the name of the
    application as a build parameter. The image-builder then builds and pushes the application's Docker image to DockerHub.

    For applications that do not have source code repositories, this job serves simply as an interim layer between the configuration-watcher
    and image-builder, because they do not have an application code repository to watch. The configuration-watcher job triggers this job, 
    and this job immediately triggers the image-builder downstream job, passing the name of the application as a build parameter. 
    The image-builder then builds and pushes the application's Docker image to DockerHub.

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.
    APPS_TO_CONFIG: a dictionary containing mappings from an edX IDA to a dictionary of various configuration values (REQUIRED). 
        The structure of APPS_TO_CONFIG should be
        
        APPS_TO_CONFIG:
            <IDA name>:
                app_repo: <GitHub repository name for the IDA>
                app_repo_branch: <branch of the above repository to checkout>
                config_branch: <GitHub branch of configuration repository to checkout>
        For example, 
        APPS_TO_CONFIG:
            ecommerce:
                app_repo: 'ecommerce'
                app_repo_branch: 'master'
                config_branch: 'master'
        Note that the default for app_repo_branch is master, and the default for config_branch is master; therefore, they do not need to be specified unless you are providing overrides.
        
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
import static org.edx.jenkins.dsl.DevopsConstants.merge_to_master_trigger
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class AppWatcher {
    public static def job = { dslFactory, extraVars ->

        //assertions come earlier to ensure APPS_TO_CONFIG can be read
        assert extraVars.containsKey('EDX_REPO_ROOT') : "Please define EDX_REPO_ROOT"
        assert extraVars.containsKey('APPS_TO_CONFIG'): "Please define APPS_TO_CONFIG"

        // for each application
        extraVars.get("APPS_TO_CONFIG").each { app_name, app_config ->
            dslFactory.job(extraVars.get("FOLDER_NAME", "DockerCI") + "/" + app_name) {

                def app_repo = app_config.get("app_repo", '')
                def app_repo_branch = app_config.get('app_repo_branch', 'master')

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

                def APP_REPO_URL = extraVars.get("EDX_REPO_ROOT") + app_repo + "/"

                // for GitHub Webhooks; note that you need to use an https URL and include
                // the trailing slash
                properties {
                    if (app_repo) {
                       githubProjectUrl(APP_REPO_URL)
                    }
                }

                scm {
                    if (app_repo) {
                        git {
                            remote {
                                url(APP_REPO_URL)
                                branch(app_repo_branch)
                            }
                            extensions {
                                relativeTargetDirectory('edx')
                            }
                        }
                    }
                }

                triggers merge_to_master_trigger(app_repo_branch)

                steps {
                    // trigger image-builder job, passing name of application as a parameter
                   downstreamParameterized {
                        trigger('image-builder') {
                            parameters {
                                predefinedProp('APP_NAME', app_name)
                            }
                        }
                    }
                }
            }
        }
    }
}
