docker run -d --name spark-master \
  -e SPARK_MODE=master \
  -p 8080:8080 \
  -p 7077:7077 \
  bitnami/spark:latest


docker run -d --name spark-worker --link spark-master:spark-master \
  -e SPARK_MODE=worker \
  -e SPARK_MASTER_URL=spark://spark-master:7077 \
  -p 8081:8081 \
  -v /home/italo/code/projects/java/pc3_spark/target/pc3_spark-1.0-SNAPSHOT.jar:/opt/spark-apps/pc3_spark.jar \
  -v ~/downloads/ojdbc8.jar:/opt/spark/jars/ojdbc8.jar \
  -v ~/downloads/hadoop-aws-3.4.1.jar:/opt/spark/jars/hadoop-aws-3.4.1.jar  -v ~/downloads/aws-java-sdk-bundle-1.11.901.jar:/opt/spark/jars/aws-java-sdk-bundle-1.11.901.jar  \
  bitnami/spark:latest

docker cp src/main/resources/secrets.properties spark-worker:/tmp/secrets.properties

docker exec -it spark-worker spark-submit \
  --master spark://spark-master:7077 \
  --class pe.edu.uni.OracleToS3 \
  --jars /opt/spark/jars/ojdbc8.jar \
  --verbose \
  /opt/spark-apps/pc3_spark.jar
