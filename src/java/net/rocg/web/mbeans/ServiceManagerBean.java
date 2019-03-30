/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.rocg.web.mbeans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import net.rocg.util.DBConnection;
import net.rocg.util.RLogger;
import net.rocg.util.RUtil;
import net.rocg.util.StatusMessage;
import net.rocg.util.StringInputValidator;
import net.rocg.web.beans.CMSService;
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author Rishi Tyagi
 */
@ManagedBean
@RequestScoped
public class ServiceManagerBean  implements java.io.Serializable{
    DBConnection dbConn;
    StatusMessage statusMsg;
    CMSService newService,selectedService;
    List<CMSService> serviceList;
    Map<String,String> serviceCategories;
    Map<String,String> ratingEngines;
    Map<String,String> ratingRules;
    Map<String,String> subscriptionCategory;
    
    /**
     * Creates a new instance of ServiceManagerBean
     */
    public ServiceManagerBean() {
        dbConn=new DBConnection();
        statusMsg=new StatusMessage();
        newService=new CMSService();
        selectedService=new CMSService();
        serviceList=new ArrayList<>();
        serviceCategories=new HashMap<>();
        ratingEngines=new HashMap<>();
        ratingRules=new HashMap<>();
        subscriptionCategory=new HashMap<>();
        refreshList(null,0,true,true,true,true,true);
        dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: MBean Connected.");
    }

    public void reloadServiceList(){
        //refreshList(null,0,true,true,true,true,true);
    }
    public void refreshList(CMSService actionObj,int action,boolean reloadServiceList,boolean reloadServiceCategories,boolean reloadRatingEngines,boolean reloadRatingRules,boolean reloadSubscriptionCategories){
        Connection conn=dbConn.connect();
        
        if(conn!=null){
                dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: refreshList() :: Database Connected.");
                try{
                    Statement st=null;
                    if(action==1){
                        //Create New
                          
                        String sql1="INSERT tb_services(service_name,service_description,service_catg,subscription_type,rating_rule,STATUS,reg_date,last_update) "
                                + "values(?,?,?,?,?,1,now(),now());";
                        dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: refreshList() :: Action Requested=REGISTER-NEW-SERVICE; SQL : " + sql1);
                        PreparedStatement pst=conn.prepareStatement(sql1);
                        pst.setString(1, actionObj.getServiceName());
                        pst.setString(2, actionObj.getServiceDescription());
                        pst.setString(3, actionObj.getServiceCategory());
                        pst.setString(4, actionObj.getSubscriptionType());
                        //pst.setInt(5, actionObj.getRatingEngineId());
                        pst.setInt(5, actionObj.getRatingRuleId());
                        int rep=pst.executeUpdate();
                        
                        pst.close();
                        pst=null;
                        if(rep>0){
                            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: refreshList() :: Action Requested=REGISTER-NEW-SERVICE; DBResp : " + rep+", New Service '"+actionObj.getServiceName()+"' registered successfully!");
                            statusMsg.setMessage("New Service '"+actionObj.getServiceName()+"' Created Successfully",StatusMessage.MSG_CLASS_INFO);
                        }else{
                            //User Created by Portal Access may not be granted
                            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: refreshList() :: Action Requested=REGISTER-NEW-SERVICE; DBResp : " + rep+", Failed to register New Service '"+actionObj.getServiceName()+"'");
                           statusMsg.setMessage("Failed to create Service '"+actionObj.getServiceName()+"'",StatusMessage.MSG_CLASS_ERROR);
                        }
                    } else if(action==2){
                        st=conn.createStatement();
                        //Update
                        String sql1="update tb_services set STATUS="+actionObj.getStatus()+",last_update =now() where service_id="+actionObj.getServiceId();
                        int rep=st.executeUpdate(sql1);
                        dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: refreshList() :: Action Requested=UPDATE-SERVICE; SQL : " + sql1+"| DBResp="+rep);
                        statusMsg.setMessage(rep>0?"Service Id ("+actionObj.getServiceId()+") updated Successfully!":"Failed to update Service id ("+actionObj.getServiceId()+")!");
                        statusMsg.setMsgClass(rep>0?StatusMessage.MSG_CLASS_INFO:StatusMessage.MSG_CLASS_ERROR);
                        
                    }else{
                        st=conn.createStatement();
                        
                       if(reloadServiceList) reloadServicesList(st);
                       //if(reloadServiceCategories) reloadServiceCategoryList(st);
                       //if(reloadRatingEngines) reloadRatingEngineList(st);
                       if(reloadRatingRules) reloadRatingRuleList(st);
                       //if(reloadSubscriptionCategories) reloadSubscriptionCategoryList(st);
                    }
                    if(st!=null) st.close();
                    st=null;
                }catch(Exception e){
                   dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: refreshList() :: Exception :" + e.getMessage());
                   statusMsg.setMessage("A process failed while creating new Service. Database Error! ",StatusMessage.MSG_CLASS_ERROR);
                   
                }finally{
                    try{if(conn!=null) conn.close();}catch(Exception ee){}
                    conn=null;
                }
            }else{
                dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: refreshList() :: Database Connectivity Failed");
                statusMsg.setMessage("A process failed while reloading service List. Database Connectivity Issue.",StatusMessage.MSG_CLASS_ERROR);
            }
       
    
    }
    
    /**
      * Edit Event Handler Method
      */
     public void onEdit(RowEditEvent event) {  
         
      CMSService actionObj=(CMSService)event.getObject();
       if(actionObj!=null && actionObj.getServiceId()>0){
           statusMsg.setMessage("Service Details sent for update into Registry!", StatusMessage.MSG_CLASS_INFO);
           refreshList(actionObj,2,true,false,false,false,false);
       }else{
           dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: onEdit() :: Invalid Service Id (" +  actionObj.getServiceId() + ")");
          statusMsg.setMessage("Invalid Service Id `" + actionObj.getServiceId() + "`. Please try again.", StatusMessage.MSG_CLASS_ERROR); 
       }
       
    }  

     
     
    /**
    * Method will create new Service into database based on the details provided in the newService, If not created the statusMsg
     */
    
    public void createNew(){
        
        newService.setServiceName(newService.getServiceName()==null?"":newService.getServiceName().trim());
        newService.setServiceDescription(newService.getServiceDescription()==null? "" : newService.getServiceDescription().trim());
       
        newService.setRatingRuleId(RUtil.strToInt(newService.getRatingRule(), 0));
       
       dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: createNew() :: New Service Name to Register '" + newService.getServiceName() + "'");
       String pV=StringInputValidator.validateString(newService.getServiceName(), "<>&@#$!`'^*?/=");
        String pDV=StringInputValidator.validateString(newService.getServiceDescription(), "<>&@#$!`'^*?/=");
        if(!pV.equalsIgnoreCase(newService.getServiceName()) || !pDV.equalsIgnoreCase(newService.getServiceDescription())){
           dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: createNew() :: Invalid Service Name ");
           statusMsg.setMessage("Sorry! Invalid Input. Special Characters are not allowed.",StatusMessage.MSG_CLASS_ERROR);
       }else 
        if(newService.getServiceName().length()<=0){
           dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: createNew() :: Invalid Service Name `" + newService.getServiceName() + "`. Service Name can't be blank.");
           statusMsg.setMessage("Invalid Service Name! Service Name can not be blank.",StatusMessage.MSG_CLASS_ERROR);
       }else if(newService.getServiceDescription().length()<=0){
           dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: createNew() :: Invalid Service Description `" + newService.getServiceDescription() + "`. Service Description can't be blank.");
           statusMsg.setMessage("Invalid Service Description! Service Description can not be blank.",StatusMessage.MSG_CLASS_ERROR);
       }else if(newService.getServiceCategory().length()<=0){
           dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: createNew() :: Invalid Service Category `" + newService.getServiceCategory() + "`. Select a valid service category.");
           statusMsg.setMessage("Invalid Service Category! Select a valid service category."+newService.getServiceCategory(),StatusMessage.MSG_CLASS_ERROR);
       }else if(newService.getSubscriptionType().length()<=0){
           dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: createNew() :: Invalid Subscription Category/Type `" + newService.getSubscriptionType() + "`. Select a valid subscription type/category.");
           statusMsg.setMessage("Invalid Subscription Type! Select a valid subscription type.",StatusMessage.MSG_CLASS_ERROR);
       }else if(newService.getRatingRuleId()<=0){
           dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: createNew() :: Invalid Rating Rule `" + newService.getRatingRuleId() + "`. Select a valid rating rule for charging.");
           statusMsg.setMessage("Invalid Rating Rule! Select a valid Rating Rule.",StatusMessage.MSG_CLASS_ERROR);
       }else{
           statusMsg.setMessage("New Service '" + newService.getServiceName() + "' sent for registration.",StatusMessage.MSG_CLASS_INFO);
           refreshList(newService,1,true,false,false,false,false);
       }
    }
    
    
    public void reloadServicesList(java.sql.Statement st){
         java.sql.ResultSet rs=null;
         serviceList.clear();
        try{
            String sql1="SELECT S.service_id,S.service_name,S.service_description,S.service_catg AS service_catg_name,S.`subscription_type` AS subscription_type_name,S.rating_rule AS default_rating_rule_id,RR.rule_name AS default_rating_rule_name,S.status FROM tb_services S,tb_rating_rules RR  WHERE  S.`rating_rule`=RR.`rule_id` ORDER BY S.`status` DESC,S.`service_name` ASC;";
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadServicesList() ::  SQL : " + sql1);
            rs=st.executeQuery(sql1);
            
            CMSService newObj=null;
            while(rs.next()){
                newObj=new CMSService();
                newObj.setServiceId(rs.getInt("service_id"));
                newObj.setServiceName(rs.getString("service_name"));
                newObj.setServiceDescription(rs.getString("service_description"));
                newObj.setServiceCategory(rs.getString("service_catg_name"));
                newObj.setSubscriptionType(rs.getString("subscription_type_name"));
                //newObj.setRatingEngineId(rs.getInt("default_rating_engine_id"));
                //newObj.setRatingEngine(rs.getString("default_rating_engine_name"));
                newObj.setRatingRuleId(rs.getInt("default_rating_rule_id"));
                newObj.setRatingRule(rs.getString("default_rating_rule_name"));
                newObj.setStatus(rs.getInt("status"));
                serviceList.add(newObj);
                newObj=null;
            }
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadServicesList() ::  Service List (ItemCount=" + serviceList.size() + ") Reloaded. ");
        }catch(Exception e){
            dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: reloadServicesList() ::  Exception  "+e.getMessage());
        }finally{
            try{ if(rs!=null) rs.close();}catch(Exception ee){}
            rs=null;
        }   
    }
    
    public void reloadServiceCategoryList(java.sql.Statement st){
        java.sql.ResultSet rs=null;
         serviceCategories.clear();
        try{
            String sql1="SELECT service_catg_id,service_catg_name FROM tb_service_categories WHERE STATUS>0 ORDER BY service_catg_name;";
            rs=st.executeQuery(sql1);
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadServiceCategoryList() ::  SQL : " + sql1);
            while(rs.next()){
                serviceCategories.put(rs.getString("service_catg_name"), rs.getString("service_catg_id"));
            }
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadServiceCategoryList() ::  Service Category List (ItemCount=" + serviceCategories.size() + ") Reloaded. ");
        }catch(Exception e){
            dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: reloadServiceCategoryList() ::  Exception  "+e.getMessage());
        }finally{
            try{ if(rs!=null) rs.close();}catch(Exception ee){}
            rs=null;
        }
    }
    
    public void reloadRatingEngineList(java.sql.Statement st){
         java.sql.ResultSet rs=null;
         ratingEngines.clear();
        try{
            String sql1="SELECT rating_engine_id,engine_name FROM tb_rating_engine WHERE STATUS>0 ORDER BY engine_name;";
            rs=st.executeQuery(sql1);            
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadRatingEngineList() :: SQL : " + sql1);
            while(rs.next()){
                ratingEngines.put(rs.getString("engine_name"), rs.getString("rating_engine_id"));
            }
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadRatingEngineList() ::  Rating Engine List (ItemCount=" + ratingEngines.size() + ") Reloaded. ");
        }catch(Exception e){
           dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: reloadRatingEngineList() ::  Exception  "+e.getMessage());
        }finally{
            try{ if(rs!=null) rs.close();}catch(Exception ee){}
            rs=null;
        }
    }
    
    public void reloadRatingRuleList(java.sql.Statement st){
        java.sql.ResultSet rs=null;
         ratingRules.clear();
        try{
            String sql1="SELECT rule_id,rule_name FROM tb_rating_rules WHERE STATUS>0 ORDER BY rule_name;";
            rs=st.executeQuery(sql1);  
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadRatingRuleList() ::  SQL : " + sql1);
            while(rs.next()){
                ratingRules.put(rs.getString("rule_name"), rs.getString("rule_id"));
            }
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadRatingRuleList() ::  Rating Rules List (ItemCount=" + ratingRules.size() + ") Reloaded. ");
        }catch(Exception e){
            dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: reloadRatingRuleList() ::  Exception  "+e.getMessage());
        }finally{
            try{ if(rs!=null) rs.close();}catch(Exception ee){}
            rs=null;
        }

    }
    
    public void reloadSubscriptionCategoryList(java.sql.Statement st){
         java.sql.ResultSet rs=null;
         subscriptionCategory.clear();
        try{
            String sql1="SELECT subs_catg_id,subs_catg_name FROM tb_subscription_categories WHERE STATUS>0 ORDER BY subs_catg_name;";
            rs=st.executeQuery(sql1);            
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadSubscriptionCategoryList() ::  SQL : " + sql1);
            while(rs.next()){
                subscriptionCategory.put(rs.getString("subs_catg_name"), rs.getString("subs_catg_id"));
            }
            dbConn.logUIMsg(RLogger.MSG_TYPE_INFO, RLogger.LOGGING_LEVEL_DEBUG, "ServiceManagerBean.class :: reloadSubscriptionCategoryList() ::  Subscription Category List (ItemCount=" + subscriptionCategory.size() + ") Reloaded. ");
        }catch(Exception e){
            dbConn.logUIMsg(RLogger.MSG_TYPE_ERROR, RLogger.LOGGING_LEVEL_ERROR, "ServiceManagerBean.class :: reloadSubscriptionCategoryList() ::  Exception  "+e.getMessage());
        }finally{
            try{ if(rs!=null) rs.close();}catch(Exception ee){}
            rs=null;
        }
    }
     
    public CMSService getNewService() {
        return newService;
    }

    public void setNewService(CMSService newService) {
        this.newService = newService;
    }

    public CMSService getSelectedService() {
        return selectedService;
    }

    public void setSelectedService(CMSService selectedService) {
        this.selectedService = selectedService;
    }

    public List<CMSService> getServiceList() {
        return serviceList;
    }

    public void setServiceList(List<CMSService> serviceList) {
        this.serviceList = serviceList;
    }

    public StatusMessage getStatusMsg() {
        return statusMsg;
    }

    public void setStatusMsg(StatusMessage statusMsg) {
        this.statusMsg = statusMsg;
    }

    public Map<String, String> getServiceCategories() {
        return serviceCategories;
    }

    public void setServiceCategories(Map<String, String> serviceCategories) {
        this.serviceCategories = serviceCategories;
    }

    public Map<String, String> getRatingEngines() {
        return ratingEngines;
    }

    public void setRatingEngines(Map<String, String> ratingEngines) {
        this.ratingEngines = ratingEngines;
    }

    public Map<String, String> getRatingRules() {
        return ratingRules;
    }

    public void setRatingRules(Map<String, String> ratingRules) {
        this.ratingRules = ratingRules;
    }

    public Map<String, String> getSubscriptionCategory() {
        return subscriptionCategory;
    }

    public void setSubscriptionCategory(Map<String, String> subscriptionCategory) {
        this.subscriptionCategory = subscriptionCategory;
    }

    
    
}