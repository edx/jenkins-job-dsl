package devops.jobs

/* JenkinsPublicConstants can be found in src/main/groovy/org/edx/jenkins/dsl*/
/* It contains values and methods that are used repeatedly through the dsl jobs */
import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

/*
A config file with the following data will need to be available at the location specified in credentialsBidning below :
    client_id: '<secret>'
    client_secret: '<secret>'
    base_urls:
        lms: http://localhost:18000
        ecommerce: http://localhost:18130
        discovery: http://localhost:18381
        credentials: http://localhost:18150
*/
class ReplaceUsernames {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('ENVIRONMENTS') : "Please define ENVIRONMENTS. It should be a list of strings."
        assert !(extraVars.get('ENVIRONMENTS') instanceof String) : "Make sure ENVIRONMENTS is a list and not a string"

        extraVars.get('ENVIRONMENTS').each { environment ->
            dslFactory.job("Enterprise/" + "${environment}-replace-usernames") {
                parameters {
                    stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
                    fileParam(
                        'username_replacements.csv',
                        'A CSV of [current_username,desired_username] for username replacement'
                    )
                }

                wrappers {
                    credentialsBinding {
                        def variable = "tools-edx-jenkins-username-replacement-${environment}-config"
                        file('USERNAME_REPLACEMENT_CONFIG_FILE',variable)
                    }
                }

                multiscm {
                    git {
                        remote {
                            url('https://github.com/edx/tubular.git')
                        }
                        branch('$TUBULAR_BRANCH')
                        extensions {
                            relativeTargetDirectory('tubular')
                            cloneOptions {
                                shallow()
                                timeout(10)
                            }
                            cleanBeforeCheckout()
                        }
                    }
                }

                wrappers common_wrappers
                logRotator common_logrotator
                concurrentBuild()

                /* Build Steps */
                steps {
                    virtualenv {
                        pythonName('System-CPython-3.5')
                        name('username-replacement')
                        nature('shell')
                        systemSitePackages(false)
                        command(dslFactory.readFileFromWorkspace('devops/resources/replace-usernames.sh'))
                    }
                }

                /* Post Build Steps */
                /* Archive artifacts, archive jUnit Reports, send an email, message on slack */
                publishers {
                    /* Save CSV from python script as artifact to be downloaded/viewed */
                    archiveArtifacts {
                        pattern('username_replacement_results.csv')
                        allowEmpty()
                        defaultExcludes()
                    }
                }

                environmentVariables {
                    env('ENVIRONMENT', environment)
                }
            }
        }
    }
}
