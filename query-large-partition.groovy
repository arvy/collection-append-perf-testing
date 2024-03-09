#! /usr/bin/groovy
@Grab('com.datastax.oss:java-driver-core:4.13.0')
@Grab('commons-lang:commons-lang:2.6')

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.cql.Row
import java.nio.file.Paths
import java.util.Random
import org.apache.commons.lang.RandomStringUtils



def cli = new CliBuilder()
cli.m('Use map')
cli.h('Display usage')
cli.c(argName: 'contactPoint', type: String, longOpt: 'contactPoint', required: true, 'Host to act as contact point of cluster to connect to')
cli.a(argName: 'astra', type: Boolean, required: false, 'Target size', defaultValue: false)
def options = cli.parse(args)
assert options // would be null (false) on failure

if( !options || options.h  ){
  cli.usage()
  System.exit(0)
}



builder = CqlSession.builder()
if( options.a ){
  println("Connecting to Astra..")
  builder = builder
    .withCloudSecureConnectBundle(Paths.get("/Users/arvydas.jonusonis/Downloads/secure-connect-map-hotpartition-test.zip"))
    .withAuthCredentials("FJhrKTcquNxDCFUFxuvBYZoE","tSh0.mUXtQA7WekmMcAN.D.5BtGhkZia5Du44Z8R0cQJ-uMy_3+bb4ZUuGqri5Nsqu_ax8poo.+-DjwRZFta3ks1XQCkXUI7Si0Y15Okdfu.7ASTOzcthjsNY-6b0IbU")
}else{
  println("Connecting to DSE/C* at ${options.c}")
  builder = builder
    .addContactPoint( new InetSocketAddress(options.c, 9042))
    .withLocalDatacenter("Cassandra")
}

try (CqlSession session = builder.build()) {
      

    ResultSet rs = session.execute("SELECT token_map FROM test.token_meta WHERE key = 'x'")
    currentSize = rs.one().getMap('token_map', String.class, String.class).size()
    println("map is of size " + currentSize)
            
} catch(Exception e){
   e.printStackTrace()
}
