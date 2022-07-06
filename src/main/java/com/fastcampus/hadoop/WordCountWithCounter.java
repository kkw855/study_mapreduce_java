package com.fastcampus.hadoop;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class WordCountWithCounter extends Configured implements Tool {
  static enum Word {
    WITHOUT_SPECIAL_CHARACTER,
    WITH_SPECIAL_CHARACTER
  }
  public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {
    private final Text word = new Text();
    private final IntWritable one = new IntWritable(1);
    private final Pattern pattern = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);

    @Override
    protected void map(
        Object key, Text value, Mapper<Object, Text, Text, IntWritable>.Context context)
        throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());

      while (itr.hasMoreTokens()) {
        String str = itr.nextToken().toLowerCase();
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
          context.getCounter(Word.WITH_SPECIAL_CHARACTER).increment(1);
        } else {
          context.getCounter(Word.WITHOUT_SPECIAL_CHARACTER).increment(1);
        }
        word.set(str);
        context.write(word, one);
        // (hadoop, 1)
      }
    }
  }

  public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
    private final IntWritable result = new IntWritable();

    @Override
    protected void reduce(
        Text key,
        Iterable<IntWritable> values,
        Reducer<Text, IntWritable, Text, IntWritable>.Context context)
        throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      context.write(key, result);
      // (hadoop, 3)
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    Job job = Job.getInstance(getConf(), "wordCount with counter");

    job.setJarByClass(WordCountWithCounter.class);

    job.setMapperClass(TokenizerMapper.class);
    job.setReducerClass(WordCount.IntSumReducer.class);

    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);

    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));

    return job.waitForCompletion(true) ? 0 : 1;
  }

  public static void main(String[] args) throws Exception {
    int exitCode = ToolRunner.run(new WordCountWithCounter(), args);
    System.exit(exitCode);
  }
}
