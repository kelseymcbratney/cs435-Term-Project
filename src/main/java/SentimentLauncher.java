package SentimentLauncher;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.Counter;

import java.net.URI;

public class SentimentLauncher extends Configured implements Tool {
  public int run(String[] args) throws Exception {
    if (args.length != 3) {
      System.err.println("Usage: SentimentLauncher <input path> <output path> <stopwords file>");
      return -1;
    }

    Configuration conf = getConf();

    Job job1 = Job.getInstance(conf, "TF Job");

    job1.setJarByClass(TFIDFJob.class);
    job1.addCacheFile(new URI(args[2]));

    // Set up the first map-reduce job to calculate term frequency (TF)
    job1.setMapperClass(TFIDFJob.TFTokenizer.class);
    job1.setMapOutputKeyClass(Text.class);
    job1.setMapOutputValueClass(Text.class);
    job1.setReducerClass(TFIDFJob.TFReducer.class);
    job1.setOutputKeyClass(Text.class);
    job1.setOutputValueClass(Text.class);

    FileInputFormat.addInputPath(job1, new Path(args[0]));
    FileOutputFormat.setOutputPath(job1, new Path(args[1] + "/tf"));

    job1.waitForCompletion(true);

    Job job2 = Job.getInstance(conf, "TFIDF Job");

    Counter totalRecordsCounter = job1.getCounters().findCounter(TFIDFJob.Counters.TOTAL_RECORDS);

    job2.getConfiguration().set("total_records", Long.toString(totalRecordsCounter.getValue()));
    job2.setJarByClass(TFIDFJob.class);

    job2.setMapperClass(TFIDFJob.TFIDFMapper.class);
    job2.setMapOutputKeyClass(Text.class);
    job2.setMapOutputValueClass(Text.class);
    job2.setReducerClass(TFIDFJob.TFIDFReducer.class);
    job2.setOutputKeyClass(Text.class);
    job2.setOutputValueClass(Text.class);

    FileInputFormat.addInputPath(job2, new Path(args[1] + "/tf"));
    FileOutputFormat.setOutputPath(job2, new Path(args[1] + "/tfidf"));

    return job2.waitForCompletion(true) ? 0 : 1;
  }

  public static void main(String[] args) throws Exception {
    int exitCode = ToolRunner.run(new SentimentLauncher(), args);
    System.exit(exitCode);
  }
}
