/*
    This job serves to build an application's Docker image and to push it to Docker Hub. This job only serves as a downstream job to the 
    various <app>-watcher jobs and does not run on its own. This job receives the name of an application as a build parameter. 
    This application's Docker image is then built and pushed to DockerHub.

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.
    
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
        dslFactory.job(extraVars.get("FOLDER_NAME", "DockerCI") + "/image-builder") {

            assert extraVars.containsKey('CONFIGURATION_REPO_URL') : "Please define CONFIGURATION_REPO_URL"

            def config_branch = 'master'

            wrappers common_wrappers
            // add credentials to log in to DockerHub; ID refers to credential ID as set up in Jenkins
            wrappers {
                credentialsBinding {
                    file("CONFIG_JSON_FILE", extraVars.get("CONFIG_JSON_FILE_CREDENTIAL_ID", "docker-config-json"))
                }
            }
            logRotator common_logrotator

            // add APP_NAME as parameter; will be passed in from upstream job as application Dockerfile to build
            parameters{
                stringParam('APP_NAME')
            }

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

            steps {
                // run the build-push-app shell script in a virtual environment called venv
                virtualenv {
                    name('venv')
                    nature('shell')
                    command dslFactory.readFileFromWorkspace('devops/resources/build-push-app.sh')
                }
            }
        }
    }
}
