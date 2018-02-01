import hudson.model.*
import hudson.triggers.*

for(item in Hudson.instance.items) {
    for(trigger in item.triggers.values()) {
        if(trigger instanceof TimerTrigger) {
            if (build.getEnvironment(listener).get('CRON_STATE').equals("ON")){
                item.enable()
            } else {
                item.disable()
            }
        }
    }
}
