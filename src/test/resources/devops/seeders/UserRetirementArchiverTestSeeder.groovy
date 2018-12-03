import static devops.jobs.UserRetirementArchiver.job as UserRetirementArchiverJob

Map jobVars = [
    MAILING_LIST: 'email@example.com',
    SECURE_GIT_CREDENTIALS: 'fake-git-credential',
    ACCESS_CONTROL: ['test-group-1'],
    ADMIN_ACCESS_CONTROL: ['test-group-2'],
    DISABLED: true,
    ENVIRONMENT_DEPLOYMENT: 'env-deployment-1',
    CRON: '0 9 * * *'
]
UserRetirementArchiverJob(this, jobVars)
