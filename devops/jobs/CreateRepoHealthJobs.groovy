package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.DevopsTasks.common_read_permissions


class CreateRepoHealthJobs{
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","RepoHealth") + "/org-repo-health-report") {

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

            def edx_repo_health_gitURL = 'git@github.com:edx/edx-repo-health.git'
            def destination_repo_health_gitURL = "git@github.com:edx/repo-health-data.git"
            def testeng_ci_gitURL = 'https://github.com/edx/testeng-ci.git'
            def repo_tools_gitURL = 'git@github.com:edx/repo-tools.git'
            
            description('Generate a report listing repository structure standard compliance accross edX repos')
            concurrentBuild(false)
            parameters {
                stringParam('EDX_REPO_HEALTH_BRANCH', 'master', 'Branch of the edx-repo-health repo to check out.')
                stringParam('ONLY_CHECK_THIS_REPOSITORY', '', 'If you only want to run repo health on one repository, set this to org/name of said repository.')
                stringParam('REPORT_DATE','','The date for which repo health data is required.(format: YYYY-MM-DD)')
            }
            multiscm {
                git {
                    remote {
                        credentials('edx-secure')
                        url(edx_repo_health_gitURL )
                    }
                    branch('$EDX_REPO_HEALTH_BRANCH')
                    browser()
                    extensions {
                        cleanBeforeCheckout()
                        relativeTargetDirectory('edx-repo-health')
                    }
                }
                git {
                    remote {
                        credentials('edx-secure')
                        url(testeng_ci_gitURL)
                    }
                    branch('master')
                    extensions {
                        cleanBeforeCheckout()
                        relativeTargetDirectory('testeng-ci')
                    }
                }
                git {
                    remote {
                        credentials('edx-secure')
                        url(repo_tools_gitURL)
                    }
                    branch('master')
                    extensions {
                        cleanBeforeCheckout()
                        relativeTargetDirectory('repo_tools')
                    }
                }
                git {
                    remote {
                        credentials('edx-secure')
                        url(destination_repo_health_gitURL )
                    }
                    branch('master')
                    browser()
                    extensions {
                        wipeOutWorkspace()
                        cleanBeforeCheckout()
                        relativeTargetDirectory('repo-health-data')
                    }
                }
            }
            triggers {
                cron('H H(4-11) * * 1-5')
            }
            wrappers {
                timeout {
                    absolute(90)
                }
                credentialsBinding {
                    string('GITHUB_TOKEN', 'GITHUB_REPOHEALTH_STATUS_BOT_TOKEN')
                    string('READTHEDOCS_API_KEY', 'READTHEDOCS_API_KEY')
                    file('REPO_HEALTH_GOOGLE_CREDS_FILE', 'REPO_HEALTH_GOOGLE_CREDS_FILE')
                }
                timestamps()
                sshAgent('edx-secure')
            }
            environmentVariables {
                env('REPO_HEALTH_OWNERSHIP_SPREADSHEET_URL', extraVars.get('SPREADSHEET_URL'))
                env('REPO_HEALTH_REPOS_WORKSHEET_ID', extraVars.get('WORKSHEET_ID'))
                env('REPORT_DATE', '$REPORT_DATE')
                env('EDX_REPO_HEALTH_BRANCH', '$EDX_REPO_HEALTH_BRANCH')
                env('ONLY_CHECK_THIS_REPOSITORY', '$ONLY_CHECK_THIS_REPOSITORY')
            }

            steps{
                shell(dslFactory.readFileFromWorkspace('devops/resources/run-repo-health-on-org.sh'))
            }

            publishers {
                archiveArtifacts {
                    pattern('repo-health-data/dashboards/*.csv')  // csv dashboards containing aggregated data
                }
                if (extraVars.get('NOTIFY_ON_FAILURE')){
                        publishers {
                            mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
                        }
                }
            }
            

        }
    }
}

