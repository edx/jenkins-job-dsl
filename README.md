# jenkins-job-dsl

## File structure

    .
    ├── <project>                       # Example project name e.g. mobile app
    │   ├── jobs                        # Job DSL code
    │   ├── resources                   # Resources specific to this project
    ├── <other_project>                 # Another project name e.g. platform
    │   ├── jobs                        # Job DSL code
    │   ├── resources                   # Resources specific to this project
    ├── <...>                           # A folder for every project!
    ├── src                             # Code shared between each project
    │   ├── main
    │   │   ├── groovy                  # support classes
    │   │   └── resources
    │   │       └── idea.gdsl           # IDE support for IDEA
    │   └── test
    │       └── groovy                  # specs (organized by project)
    │           └──<project>
    │           └──<other_project>
    └── build.gradle                    # build file
    └── seeds 
        └── <example>.groovy            # Groovy script to create jobs for an individual machine
        └── <other>.groovy              # Groovy script to create jobs for an individual machine


## Testing

### Spec Testing
DSL jobs can be tested using the [Spock test framework](http://spockframework.github.io/spock/docs/1.0/index.html). 

To run all of the test specs:
`./gradlew test`
To run all of the test specs within a given project:
`./gradlew test --tests sample.*`
To run all a single test spec:
`./gradlew test --tests sample.SampleJobSpec`

### Static Analysis
You can run codenarc to verify well written Groovy code.

To run codenarc on the dsl jobs:
`./gradlew codenarcJobs`
To run codenarc on the shared code:
`./gradlew codenarcSrc`
etc...

### Debug XML
You can pass the dsl scripts to a local Jenkins instance and generate the actual XML for a job configuration. Currently, this is only useful for verifying that it doesn't blow up, and eyeballing the XML for a sanity test.

To generate the XML config files for all of the DSL scripts within a project:
`./gradlew debugXml -Dpattern=<project>/jobs/*.groovy`

