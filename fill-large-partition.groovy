#! /usr/bin/groovy
@Grab('com.datastax.oss:java-driver-core:4.13.0')
@Grab('commons-lang:commons-lang:2.6')

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.cql.Row
import java.nio.file.Paths
import java.util.Random
import org.apache.commons.lang.RandomStringUtils


def cluster = null

Double outlierKeyChance = new Double("0.50");

def cli = new CliBuilder()
cli.m('Use map')
cli.h('Display usage')
cli.c(argName: 'contactPoint', type: String, longOpt: 'contactPoint', required: true, 'Host to act as contact point of cluster to connect to')
cli.s(argName: 'collectionSize', type: String, required: false, 'Target size', defaultValue: "1000000")
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

try (CqlSession session = builder.build() ) {

    targetSize = Integer.parseInt(options.s)
    if( !options.a ){
      session.execute("CREATE KEYSPACE IF NOT EXISTS test WITH replication = {'class':'SimpleStrategy', 'replication_factor': 1};")
      session.execute("CREATE TABLE IF NOT EXISTS test.token_meta(key text PRIMARY KEY, token_map map<text,text>);")
    }
    println('----- Entering loop... -----')
    Random rand = new Random()
    int i=0
    int checkpointInterval=10000
    while(true){
      println("i=${i}")

        double d = rand.nextDouble()
        String randomMapKey = RandomStringUtils.random(10, true, true)
        String randomMapValue = RandomStringUtils.random(10, true, true)

        if( i >= checkpointInterval && i % checkpointInterval == 0 ){
          try{
            ResultSet rs = session.execute("SELECT token_map FROM test.token_meta WHERE key = 'x'")
            currentSize = rs.one().getMap('token_map', String.class, String.class).size()
            println("map is of size " + currentSize)
            if( currentSize > targetSize ){
              System.exit(0)
            }
            }catch(Throwable e){
              println(e)
            }
        }
        session
        .executeAsync("update test.token_meta set token_map = token_map + {'$randomMapKey':'$randomMapValue'} where key = 'x';")
        .exceptionally( ex -> {
          println(ex)
          if( ex instanceof com.datastax.oss.driver.api.core.NoNodeAvailableException ){
            println("sleeping for 10s")
            Thread.sleep(10)
          }
          
          }
         ).thenAccept(asyncResultSet -> {
                  println("item inserted")  
                })
        i++
      // }else{
              // session.executeAsync("insert into test.token_meta2(key, ckey, value) values ('x', '$randomMapKey','$randomMapValue'};")
            // }
        // } else {
        //     int alphaOffset = rand.nextInt(26)
        //     String lowercaseAlphaLetter = String.valueOf((char)97+alphaOffset)
        //     System.out.printf("%s - ", lowercaseAlphaLetter)
        //     if( options.m ){
        //       session.executeAsync("update test.token_meta set token_map = token_map + {'" + randomMapKey + "':'" + randomMapValue + "'} where key = '" + lowercaseAlphaLetter +"'")
        //     }else{
        //       session.executeAsync("insert into test.token_meta2(key, ckey, value) values ('$lowercaseAlphaLetter','$randomMapKey', '$randomMapValue')")
        //     }
        // }
        //Thread.sleep(50)
    }
} catch(Exception e){
   e.printStackTrace()
} finally {
    System.out.println("Closing connection")
    if( cluster )
        cluster.close()
}
