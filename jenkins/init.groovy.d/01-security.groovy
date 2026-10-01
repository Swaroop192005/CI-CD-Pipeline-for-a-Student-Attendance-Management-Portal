// Create the admin user and lock the instance down, so the setup wizard can be
// skipped and the Jenkins configuration is reproducible from this repository
// rather than from clicks in a browser.
import jenkins.model.*
import hudson.security.*
import jenkins.security.s2m.AdminWhitelistRule

def instance = Jenkins.getInstance()

def user = System.getenv('JENKINS_ADMIN_ID') ?: 'admin'
def pass = System.getenv('JENKINS_ADMIN_PASSWORD') ?: 'admin123'

if (!(instance.getSecurityRealm() instanceof HudsonPrivateSecurityRealm)) {
    def realm = new HudsonPrivateSecurityRealm(false)
    realm.createAccount(user, pass)
    instance.setSecurityRealm(realm)

    def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
    strategy.setAllowAnonymousRead(false)
    instance.setAuthorizationStrategy(strategy)

    instance.save()
    println "--> init: created admin user '${user}' and enabled matrix-free auth"
}
