// Register the JDK that the image ships and the Maven mounted from the host.
//
// The image is jenkins/jenkins:lts-jdk17, so JDK 17 is already present at
// /opt/java/openjdk. Maven is bind-mounted because the image has none and this
// environment blocks both package installation and the Jenkins tool downloads.
import jenkins.model.*
import hudson.model.JDK
import hudson.tasks.Maven

def instance = Jenkins.getInstance()

def jdkDesc = instance.getDescriptorByType(hudson.model.JDK.DescriptorImpl)
if (jdkDesc.getInstallations().length == 0) {
    jdkDesc.setInstallations(new JDK("jdk17", "/opt/java/openjdk"))
    jdkDesc.save()
    println "--> init: registered JDK 'jdk17' at /opt/java/openjdk"
}

def mvnDesc = instance.getDescriptorByType(Maven.DescriptorImpl)
if (mvnDesc.getInstallations().length == 0) {
    mvnDesc.setInstallations(new Maven.MavenInstallation("maven3", "/opt/maven", []))
    mvnDesc.save()
    println "--> init: registered Maven 'maven3' at /opt/maven"
}
