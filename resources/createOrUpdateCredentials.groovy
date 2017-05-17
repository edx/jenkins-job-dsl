// Creates or updates a GitHub credential.
//
// Examples
// curl --data-urlencode "script=$(<./resources/createOrUpdateCredentials.groovy)" http://localhost:8080/scriptText
// curl --user 'username:password' --data-urlencode "script=$(<./resources/createOrUpdateCredentials.groovyentials.groovy)" http://localhost:8080/scriptText

import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl

String id = 'github'
String description = 'GitHub'

// TODO Set these values to your own GitHub user name and access token. See https://github.com/settings/tokens.
// TODO Do NOT commit your credentials!
String username = '#SET-ME'
String password = '#SET-ME'

def domain = Domain.global()
def credentialStore = SystemCredentialsProvider.getInstance().getStore()
def credentials = CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, Jenkins.instance)
def oldCredential = credentials.findResult { it.id == id ? it : null }
Credentials credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, username, password)

if (oldCredential) {
    credentialStore.updateCredentials(domain, oldCredential, credential)
    println("Updated existing credential: ${id}")
} else {
    credentialStore.addCredentials(domain, credential)
    println("Created new credential: ${id}")
}
