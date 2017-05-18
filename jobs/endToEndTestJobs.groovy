import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions
import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers


def generateEcommerceConfig = { environmentName, authenticationToken ->
    return [
        'name'                : "${environmentName}-ecommerce",
        'repositories'        : [
            [
                'url'       : 'https://github.com/edx/ecommerce.git',
                'branch'    : 'master',
                'credential': null,
                'directory' : '',
            ],
            [
                'url'       : 'git@github.com:edx-ops/ecom-secure.git',
                'branch'    : 'master',
                'credential': 'learner-secure-github-pull-ssh-key',
                'directory' : 'ecom-secure',
            ],

        ],
        'pythonName'          : 'System-CPython-2.7',
        'environmentVariables': [
            'DOTENV_PATH': "ecom-secure/jenkins/e2e/${environmentName}-ecommerce.env",
        ],
        'acls'                : [
            'edx*jenkins-tools-e2e-test-jobs',
        ],
        // NOTE: This requires activating global security. See http://stackoverflow.com/a/21057483/592820.
        'authenticationToken' : authenticationToken,
    ]
}
final testJobs = [
    generateEcommerceConfig('stage', 'SETME'),
]

testJobs.each { testJob ->
    String jobName = "${testJob.name}-e2e"

    job(jobName) {
        description("End-to-end tests for ${testJob.name}")
        wrappers common_wrappers
        environmentVariables {
            envs(testJob.environmentVariables)
        }
        authenticationToken(testJob.authenticationToken)

        testJob.acls.each { acl ->
            common_read_permissions.each { perm ->
                authorization {
                    permission(perm, acl)
                }
            }
        }

        multiscm {
            testJob.repositories.each { repository ->
                git {
                    remote {
                        url(repository.url)
                        credentials(repository.credential)
                    }
                    branch(repository.branch)
                    extensions {
                        cleanAfterCheckout()
                        cloneOptions {
                            shallow(true)
                        }
                        pruneBranches()
                        if (repository.directory) {
                            relativeTargetDirectory(repository.directory)
                        }
                    }
                }
            }
        }

        steps {
            virtualenv {
                name(jobName)
                pythonName(testJob.pythonName)
                nature('shell')
                command readFileFromWorkspace('platform/resources/end-to-end-test.sh')
            }
        }
        publishers {
            archiveArtifacts {
                pattern('e2e/*.xml') // xunit output
                pattern('*.log') // test logs
                pattern('*.png') // test screenshots
            }
            archiveJunit('e2e/*.xml')
        }
    }
}