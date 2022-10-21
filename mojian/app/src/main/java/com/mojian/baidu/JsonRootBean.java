/**
  * Copyright 2022 bejson.com 
  */
package com.mojian.baidu;
import java.util.List;

/**
 * Auto-generated: 2022-10-21 14:28:36
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
public class JsonRootBean {

    private int result_num;
    private List<Result> result;
    private long log_id;
    public void setResult_num(int result_num) {
         this.result_num = result_num;
     }
     public int getResult_num() {
         return result_num;
     }

    public void setResult(List<Result> result) {
         this.result = result;
     }
     public List<Result> getResult() {
         return result;
     }

    public void setLog_id(long log_id) {
         this.log_id = log_id;
     }
     public long getLog_id() {
         return log_id;
     }

    @Override
    public String toString() {
        return super.toString();
    }
}