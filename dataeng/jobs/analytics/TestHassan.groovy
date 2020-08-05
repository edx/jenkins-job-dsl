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
                         selectDays {
                             days {
                                 day {
                                     day(1)
                                     selected(true)
                                 }
                             }
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