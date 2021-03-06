package com.idc.dao.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import com.idc.domain.HTML;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;


public class HBaseDaoImpl {

	private static TableName tableName=TableName.valueOf("html");
	private static Connection conn;
	private static String hbaseIp = "192.168.1.109,192.168.1.127,192.168.1.128";

	static  {
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.rootdir","hdfs://192.168.1.109:9000/hbase");
		config.set("hbase.zookeeper.quorum", hbaseIp);
		try {
			conn = ConnectionFactory.createConnection(config);
			System.out.println(conn);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 *创建表
	 * @param args
	 */
	public  void createTable(String[] args){
		System.out.println("start create table....");
		Admin hBaseAdmin = null;
		try {
			hBaseAdmin = conn.getAdmin();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
//			if(hBaseAdmin.tableExists(tableName)){
//				hBaseAdmin.disableTable(tableName);
//				hBaseAdmin.deleteTable(tableName);
//				System.out.println(tableName.toString()+"is exist,delete....");
//			}
			HTableDescriptor tableDescriptor=new HTableDescriptor(tableName);
			for(int i=0;i<args.length;i++){
				tableDescriptor.addFamily(new HColumnDescriptor(args[i]));
			}
			hBaseAdmin.createTable(tableDescriptor);
			System.out.println("end create table....");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	/**
	 * 插入数据
	 * @param html
	 */
	public void insertData(HTML html){
		System.out.println("start insert data....");
        Table table= null;
        try {
            table = conn.getTable(tableName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Put put=new Put(html.getTitle().getBytes());
		put.add("title".getBytes(), null,html.getTitle().getBytes());
		put.add("description".getBytes(),null,html.getDescription().getBytes());
		put.add("date".getBytes(),null,String.valueOf(html.getDate().getTime()).getBytes());
		put.add("content".getBytes(),null,html.getContent());
		put.add("url".getBytes(), null, html.getUrl().getBytes());
		try {
			table.put(put);
			System.out.println("end insert data....");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 删除表格
	 */
	public void dropTable(){
        Admin hBaseAdmin = null;
		try {
			if(hBaseAdmin.tableExists(tableName)){
				hBaseAdmin.disableTable(tableName);
				hBaseAdmin.deleteTable(tableName);
				System.out.println(tableName.toString()+" has been deleted...");
			}else{
				System.out.println(tableName.toString()+" does not exist...");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 根据行键删除某条记录
	 * @param rowKey
	 */
	public  void deleteRow(String rowKey){

		try {
			Table table =conn.getTable(tableName);
			Delete delete=new Delete(rowKey.getBytes());
			table.delete(delete);
			System.out.println("delete row record success");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 查询所有元素
	 */
	public List<HTML> queryAll(){
		List<HTML> list=new ArrayList<HTML>();
        Table table = null;
        try {
            table = conn.getTable(tableName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ResultScanner rs=null;
		String title=null;
		String description=null;
		String url=null;
		Date date=null;
		HTML html=null;
		try {
			rs=table.getScanner(new Scan());
			for(Result r:rs){
				for(KeyValue keyValue:r.raw()){
					String key=new String(keyValue.getFamily());
					if(key.equals("title")){
						title=new String(keyValue.getValue());
					}else if(key.equals("description")){
						description=new String(keyValue.getValue());
					}else if(key.equals("date")){
						date=new Date(Long.parseLong(new String(keyValue.getValue())));
					}else{
						url=new String(keyValue.getValue());
					}
				}
				html=new HTML(title,description,date,null,url);
				System.out.println(html);
				list.add(html);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}finally{
			if(rs!=null){
				rs.close();
			}
		}
		return list;
	}
	
	/**
	 * 单条件单结果查询
	 * @param rowKey
	 */
	public  HTML queryByRowKey(String rowKey){
        Table table = null;
        try {
            table = conn.getTable(tableName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Get get=new Get(rowKey.getBytes());
		HTML html=null;
		String key=null;
		try {
			Result r=table.get(get);
			for(KeyValue keyValue:r.raw()){
				key=new String(keyValue.getFamily());
				if(key.equals("content")){
					html=new HTML();
					html.setContent(keyValue.getValue());
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return html;
	}
	/**
	 * 单条件多结果查询
	 */
//	public  void queryByColumn(String key){
//
//        Table table =conn.getTable(TableName.valueOf(tableName));
//		Filter filter=new SingleColumnValueFilter(Bytes.toBytes("column1"), null, CompareOp.EQUAL, Bytes.toBytes(key));
//		Scan scan=new Scan();
//		scan.setFilter(filter);
//		ResultScanner rs=null;
//		try {
//			rs=table.getScanner(scan);
//			for(Result r:rs){
//				System.out.println("rowKey:"+new String(r.getRow()));
//				for(KeyValue keyValue:r.raw()){
//					System.out.println("raw:"+new String(keyValue.getFamily())+"===value:"+new String(keyValue.getValue()));
//				}
//			}
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}finally{
//			if(rs!=null){
//				rs.close();
//			}
//		}
//	}

	public static void main(String[] args) {
		HBaseDaoImpl hBaseDao=new HBaseDaoImpl();
//		HTML html1=new HTML("poem test hhh wang test","a description keep test",new Date(),"I just test this project!".getBytes(),"www.baidu.com");
//		HTML html2=new HTML("title xiang yulin yyyy","I do not know",new Date(),"Kaipeng wang is a gay!He loves men!".getBytes(),"www.hao123.com");
//		hBaseDao.insertData(html1);
//		hBaseDao.insertData(html2);
		hBaseDao.createTable(new String[]{"testliuqi"});

	}
}
