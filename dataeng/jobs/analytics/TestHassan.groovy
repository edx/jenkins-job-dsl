package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers


class TestHassan {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("test-hassan-param") {
            parameters {
                stringParam('NOTIFY', '', '')
            }
            binding.variables.each {
              println "${it.key} = ${it.value}"
            }
            publishers {
                if ('$NOTIFYNOTEXISTS') {
                    mailer('$NOTIFY')
                }
            }
        }
    }
}