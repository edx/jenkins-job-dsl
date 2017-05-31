/*
    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    APPS_TO_REPO: a dictionary containing mappings from an edX IDA to the name of its respective GitHub repository (REQUIRED)
    APPS_TO_BRANCH: a dictionary containing mappings from an edX IDA to the name of its main GitHub branch (REQUIRED)
    CONFIGURATION_REPO_URL: URL of the configuration GitHub repository (REQUIRED)
    CONFIGURATION_BRANCH: the main GitHub branch of the configuration repository (REQUIRED)
    EDX_REPO_ROOT: the URL of the edX GitHub organization (REQUIRED)
    CONFIG_JSON_FILE_CREDENTIAL_ID: ID of the credentials set up in Jenkins to log into DockerHub; defaults to docker-config-json; this refers to the docker-config-json expected credential described below
    FOLDER_NAME: the name of the folder in which the downstream per-app jobs will live; defaults to DockerCI

    Expected credentials - these will normally be set up on the Folder.

    docker-config-json: a file credential that gives each job in the folder permission to push to DockerHub
*/

package devops.jobs

import static org.edx.jenkins.dsl.YAMLHelpers.parseYaml
import org.yaml.snakeyaml.error.YAMLException

class DockerCI {
    public static def job = { dslFactory, extraVars ->
        // for each application
        extraVars.get("APPS_TO_REPO").each { app_name, app_repo ->
            dslFactory.job(extraVars.get("FOLDER_NAME", "DockerCI") + "/" + app_name) {

                assert extraVars.containsKey('APPS_TO_REPO') : "Please define APPS_TO_REPO"
                assert extraVars.containsKey('APPS_TO_BRANCH') : "Please define APPS_TO_BRANCH"
                assert extraVars.containsKey('CONFIGURATION_REPO_URL') : "Please define CONFIGURATION_REPO_URL"
                assert extraVars.containsKey('CONFIGURATION_BRANCH') : "Please define CONFIGURATION_BRANCH"
                assert extraVars.containsKey('EDX_REPO_ROOT') : "Please define EDX_REPO_ROOT"

                // add credentials to log in to DockerHub; ID refers to credential ID as set up in Jenkins
                wrappers {
                    credentialsBinding {
                        file("CONFIG_JSON_FILE", extraVars.get("CONFIG_JSON_FILE_CREDENTIAL_ID", "docker-config-json"))
                    }
                }
                multiscm {
                    // check out edx/configuration repository from GitHub; necessary to be able to run dependency analyzer
                    git {
                        remote {
                            url(extraVars.get("CONFIGURATION_REPO_URL"))
                            branch(extraVars.get("CONFIGURATION_BRANCH"))
                        }
                        extensions {
                            relativeTargetDirectory('configuration')
                            cleanAfterCheckout()
                            // ignore notifications on commits to branch; this job will be triggered by
                            // configuration-watcher job
                            ignoreNotifyCommit()
                        }
                    }
                    // if the IDA has a corresponding repository in the edx organization, checkout that repository
                    if (app_repo) {
                        git {
                            remote {
                                url(extraVars.get("EDX_REPO_ROOT") + app_repo + '.git')
                                branch(extraVars.get("APPS_TO_BRANCH").get(app_name))
                            }
                            extensions {
                                relativeTargetDirectory('edx')
                            }
                        }
                    }
                }
            

                // polls configuration repository for changes every 10 minutes
                triggers {
                    scm('H/10 * * * *')
                }

                steps {
                    // inject name of the IDA as an environment variable
                    environmentVariables {
                        env('APP_NAME', app_name)
                    }

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
}
