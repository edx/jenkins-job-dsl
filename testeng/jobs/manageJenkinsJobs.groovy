import jenkins.model.*

// Ad-hoc job management script.

// Make sure to run in print-only mode first and get approvals before
// changing to add delete() or other calls.

Jenkins.instance.items.findAll { job ->
    // Find extraneous upgrade jobs after rename
    job.name ==~ /edx-.*-upgrade-python-requirements/ && job.disabled
}.each { job ->
    println job.name
}
