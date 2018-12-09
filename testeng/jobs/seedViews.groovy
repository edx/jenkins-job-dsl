package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

job('seed-views') {

    description('Run the platform views dsl on Jenkins startup.')

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    concurrentBuild(false)

    steps {
        downstreamParameterized {
            trigger('manually-seed-one-job') {
                parameters {
                    predefinedProp('DSL_SCRIPT', 'platform/views/*')
                }
            }
        }
    }
}
