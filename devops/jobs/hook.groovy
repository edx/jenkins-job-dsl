import static devops.jobs.CreateAprosSandbox.job as createSandboxJob

Map globals = binding.variables
String extraVarsStr = globals.get('EXTRA_VARS')
Map extraVars = [:]

createSandboxJob(this, globals + extraVars + [SANDBOX_JOB_NAME: "CreateAprosSandbox_Testing"])
