package pe.edu.uni;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.api.java.function.FilterFunction;

import scala.Function1;

import java.util.Properties;

public class TouristicLocationsJob {

    public static void main(String[] args) {
        // Validate input arguments
        if (args.length < 4) {
            System.err.println("Usage: TouristicLocationsJob <latitude> <longitude> <secretsFile> <s3OutputPath>");
            System.exit(1);
        }

        // Read input arguments
        double latitude = Double.parseDouble(args[0]);
        double longitude = Double.parseDouble(args[1]);
        String secretsFile = args[2];
        String s3OutputPath = args[3];

        // Load secrets from file
        Properties secrets = SecretsLoader.loadSecrets(secretsFile);

        String mongoUri = secrets.getProperty("mongo.uri");
        String mongoDatabase = secrets.getProperty("mongo.database");
        String mongoCollection = secrets.getProperty("mongo.collection");

        String s3AccessKey = secrets.getProperty("s3.accessKey");
        String s3SecretKey = secrets.getProperty("s3.secretKey");
        System.out.println(mongoUri);

        // Spark configuration
        SparkConf sparkConf = new SparkConf()
                .setAppName("TouristicLocationsJob")
                .setMaster("spark://spark-master:7077")
                .set("spark.mongodb.read.connection.uri", secrets.getProperty("mongo.uri"))
                .set("spark.mongodb.read.database", secrets.getProperty("mongo.database"))
                .set("spark.mongodb.read.collection", secrets.getProperty("mongo.collection"))
                .set("spark.hadoop.fs.s3a.access.key", secrets.getProperty("s3.accessKey"))
                .set("spark.hadoop.fs.s3a.secret.key", secrets.getProperty("s3.secretKey"))
                .set("spark.hadoop.fs.s3a.endpoint", "s3.amazonaws.com")
                .set("spark.hadoop.fs.s3a.path.style.access", "true");

        SparkSession sparkSession = SparkSession.builder()
                .config(sparkConf)
                .getOrCreate();

        try {
            // Read data from MongoDB
            Dataset<Row> locations = sparkSession.read()
                    .format("mongodb")
                    .load();

            // Filter locations within 1 km of the provided latitude and longitude
            // Dataset<Row> nearbyLocations = locations.filter(row -> {
            // Dataset<Row> nearbyLocations = locations.filter((Function1<Row, Object>) row
            // -> {
            Dataset<Row> nearbyLocations = locations.filter((FilterFunction<Row>) row -> {
                Row geoLocation = row.getAs("geoLocation");
                if (geoLocation == null)
                    return false;

                double[] coordinates = geoLocation.getAs("coordinates");
                double locationLatitude = coordinates[1];
                double locationLongitude = coordinates[0];

                return haversineDistance(latitude, longitude, locationLatitude, locationLongitude) <= 1.0;
            });

            // Write the filtered results to S3 as a Parquet file
            nearbyLocations.repartition(10)
                    .write()
                    .mode("overwrite")
                    .parquet(s3OutputPath);

            // Print success message
            System.out.println("Filtered data successfully written to S3 at: " + s3OutputPath);
        } finally {
            // Stop Spark session
            sparkSession.stop();
        }
    }

    /**
     * Calculate the Haversine distance between two points.
     *
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Earth radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
}
