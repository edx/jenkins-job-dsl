package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers


class TestHassan {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("test-hassan-param") {
            steps{
                conditionalSteps {
                  condition {
                    dayCondition {
                      daySelector {
                         SelectDays {
                             SelectDays([1,true])
                         }
                      }
                      useBuildTime(false)
                    }
                  }
                  runner('Fail')
                  steps {
                    shell('echo Hello')
                  }
                }
            }
        }
    }
}