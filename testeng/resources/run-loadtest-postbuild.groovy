def home = manager.envVars['HOME']
def generate_summary_script = home + '/edx-venv/bin/generate_summary'
def edx_load_tests_root = home + '/workspace/loadtest-driver/edx-load-tests'

// This is just a silly thing to convert a mapping into a list of strings, each
// containin the key value pair formatted with an equals character.
// String.execute() demands this kind of mapping instead of a legit mapping.
def envp = []
for ( e in manager.envVars.entrySet) {
    envp += "${e.getKey()}=${e.getValue()}"
}

// DEBUG
manager.listener.logger.println("running '${generate_summary_script}'.execute(_, new File('${edx_load_tests_root}'))")

// The generate_summary script expects a few of the same environment variables
// used before during the build, so just bulk copy the entire build environment
// to this one.
def proc = generate_summary_script.execute(envp, new File(edx_load_tests_root))

// Direct both stdout and stderr of the process into the console log of the
// current build.
proc.consumeProcessOutput(manager.listener.logger, manager.listener.logger)

// Set a timeout for the process.  Even 10 seconds to generate a summary is a
// really long time.
proc.waitForOrKill(10000)

/*
Map constantsMap = [:]
try {
    out.println('Parsing secret YAML file')
    String constantsConfig = new File("${EDX_PLATFORM_SHARED_CONSTANTS}").text
    Yaml yaml = new Yaml()
    constantsMap = yaml.load(constantsConfig)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}
*/
