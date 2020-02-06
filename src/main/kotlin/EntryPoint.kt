import com.github.dockerjava.core.DockerClientBuilder
import com.ccarvalho.apigateway.main
import java.net.URL

fun main(args: Array<String>) {

    var runtimeArgs = arrayOf<String>()

    if (System.getenv("CONTAINER_MANAGER") == "ECS") {
        val dockerClient = DockerClientBuilder.getInstance().build()

        // Inside a container, the HOSTNAME variable is the first characters of the container id.
        val hostName = System.getenv("HOSTNAME")

        // Find the external port for this application
        val externalPort = dockerClient.listContainersCmd().exec()
                .first { it.id.startsWith(hostName) }
                .ports
                .first { it.privatePort == 8080 }
                .publicPort

        val externalIp = URL("http://169.254.169.254/latest/meta-data/local-ipv4").readText()

        runtimeArgs = arrayOf(
                "--eureka.client.registerWithEureka=true",
                "--eureka.instance.prefer-ip-address=true",
                "--eureka.instance.ip-address=$externalIp",
                "--eureka.instance.non-secure-port=$externalPort",
                "--eureka.instance.instance-id=$externalIp:$externalPort",
                "--eureka.instance.hostname=$externalIp",
                "--eureka.instance.status-page-url=http://$externalIp:$externalPort/api-gateway/info",
                "--eureka.instance.health-check-url=http://$externalIp:$externalPort/api-gateway/health")
    }

    main(args + runtimeArgs)
}