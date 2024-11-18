package pe.edu.uni;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Properties;

public class OracleToS3 {
    public static void main(String[] args) {
        Properties secrets = SecretsLoader.loadSecrets("secrets.properties");

        String jdbcUrl = secrets.getProperty("db.url");
        String dbUser = secrets.getProperty("db.user");
        String dbPassword = secrets.getProperty("db.password");

        String s3AccessKey = secrets.getProperty("s3.accessKey");
        String s3SecretKey = secrets.getProperty("s3.secretKey");
        String s3BucketName = secrets.getProperty("s3.bucketName");

        SparkSession spark = SparkSession.builder()
                .appName("OracleToS3")
                .master("spark://172.17.0.2:7077")
                .config("spark.hadoop.fs.s3a.access.key", s3AccessKey)
                .config("spark.hadoop.fs.s3a.secret.key", s3SecretKey)
                .config("spark.hadoop.fs.s3a.endpoint", "s3.amazonaws.com")
                .config("spark.hadoop.fs.s3a.path.style.access", "true")
                // .config("spark.eventLog.enabled", "true")
                // .config("spark.eventLog.dir", "/home/italo/code/projects/java/pc3_spark/tmp/spark-events")
                .getOrCreate();

        Properties connectionProps = new Properties();
        connectionProps.put("user", dbUser);
        connectionProps.put("password", dbPassword);
        connectionProps.put("driver", "oracle.jdbc.OracleDriver");

        Dataset<Row> jdbcDF = spark.read()
                .jdbc(jdbcUrl, "TEST", connectionProps);

        String s3OutputPath = "s3a://" + s3BucketName  + "/output";
        jdbcDF.repartition(100)
                .write()
                .mode("overwrite")
                .parquet(s3OutputPath);


        jdbcDF.show();
        System.out.println("Data successfully written to S3.");
        spark.stop();
    }
}
