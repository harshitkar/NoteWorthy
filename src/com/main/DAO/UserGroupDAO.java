package com.main.DAO;

import com.main.model.User;
import com.main.model.UserGroup;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 *
 * @author Sarthak Joshi
 */
public class UserGroupDAO {

    PreparedStatement pst;
    ResultSet rs;
    Connection con;
    DataBaseConnector dataBaseConnector = new DataBaseConnector();

    public ArrayList<UserGroup> getGroupList(String currentUsername) {
        ArrayList<UserGroup> model = new ArrayList<>();
        try{
            con = dataBaseConnector.connect();
            String sql = "SELECT `group`.groupId, `group`.groupName, `user_group`.role, `user_group`.dateJoined " +
                    "FROM noteworthy.`group` " +
                    "INNER JOIN noteworthy.`user_group` ON `group`.groupId = `user_group`.groupId " +
                    "WHERE `user_group`.username = ? ;";
            pst = con.prepareStatement(sql);
            pst.setString(1, currentUsername);
            rs = pst.executeQuery();
            while(rs.next()){
                String groupId = rs.getString("groupId");
                String groupName = rs.getString("groupName");
                String dateJoined = rs.getString("dateJoined");
                UserGroup newGroup = new UserGroup(groupId, groupName, dateJoined);
                model.add(newGroup);
            }
            Collections.sort(model, (obj1, obj2) -> {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                int result = 0;
                try {
                    Date date1 = format.parse(obj1.getLastPostDate());
                    Date date2 = format.parse(obj2.getLastPostDate());
                    result =  date2.compareTo(date1);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return result;
            });
        } catch(HeadlessException | SQLException ex){
            System.out.println(ex);
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return model;
    }


    public boolean isGroupIdAvailable(String groupId) {
        try{
            con = dataBaseConnector.connect();
            try {
                String s = "SELECT * FROM `group` WHERE `group`.`groupId` = ?;";
                pst = con.prepareStatement(s);
                pst.setString(1, groupId);
                rs = pst.executeQuery();
                if(!rs.next()) {
                    return true;
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        } catch(HeadlessException ex){
            System.out.println(ex);
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean createGroup(String groupId, String groupName, String currentUsername) {
        try{
            con = dataBaseConnector.connect();
            try {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String mysqlDateTime = now.format(formatter);
                pst = con.prepareStatement("insert into `group` values(?, ?);");
                pst.setString(1, groupId);
                pst.setString(2, groupName);
                pst.executeUpdate();
                pst = con.prepareStatement("insert into `user_group`(username, groupId, role, dateJoined) values(?, ?, ?, ?);");
                pst.setString(1, currentUsername);
                pst.setString(2, groupId);
                pst.setString(3, "creator");
                pst.setString(4, mysqlDateTime);
                pst.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e);
                return false;
            }
        } catch(HeadlessException ex){
            System.out.println(ex);
            return false;
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public boolean joinGroup(String currentUsername, String groupId) {
        try{
            con = dataBaseConnector.connect();
            try {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String mysqlDateTime = now.format(formatter);
                pst = con.prepareStatement("select * from `user_group` where `groupId` = ? and `username` = ?;");
                pst.setString(1, groupId);
                pst.setString(2, currentUsername);
                rs = pst.executeQuery();
                if(!rs.next()) {
                    pst = con.prepareStatement("insert into `user_group` values(?, ?, ?, ?);");
                    pst.setString(1, currentUsername);
                    pst.setString(2, groupId);
                    pst.setString(3, "member");
                    pst.setString(4, mysqlDateTime);
                    pst.executeUpdate();
                    return true;
                }
            } catch (SQLException e) {
                System.out.println(e);
                return false;
            }
        } catch(HeadlessException ex){
            System.out.println(ex);
            return false;
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public String getCreator(String groupId) {
        try{
            con = dataBaseConnector.connect();
            pst = con.prepareStatement("SELECT `username` FROM `user_group` WHERE `groupId` = ? and role = 'creator';");
            pst.setString(1, groupId);
            rs = pst.executeQuery();
            if(rs.next()){
                return rs.getString("username");
            }
        } catch(HeadlessException | SQLException ex){
            System.out.println(ex);
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public DefaultListModel<String> getGetGroupAdminList(String currentUsername, String groupId) {
        DefaultListModel<String> model = new DefaultListModel<>();
        String username = new String();
        try{
            con = dataBaseConnector.connect();
            pst = con.prepareStatement("select `username` from `user_group` where groupId = ? and role = 'creator';");
            pst.setString(1, groupId);
            rs = pst.executeQuery();
            rs.next();
            username = rs.getString("username");
            if(!currentUsername.equals(username)) {
                model.addElement(username);
            } else {
                model.addElement("You");
            }
            pst = con.prepareStatement("select `username` from `user_group` where groupId = ? and role = 'admin';");
            pst.setString(1, groupId);
            rs = pst.executeQuery();
            while(rs.next()){
                username = rs.getString("username");
                if(!currentUsername.equals(username)) {
                    model.addElement(username);
                } else {
                    model.addElement("You");
                }
            }
        } catch(HeadlessException | SQLException ex){
            System.out.println(ex);
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return model;
    }

    public DefaultListModel<String> getGroupMemberList(String currentUsername, String groupId) {
        DefaultListModel<String> model = new DefaultListModel<>();
        String username = new String();
        try{
            con = dataBaseConnector.connect();
            pst = con.prepareStatement("select `username` from `user_group` where groupId = ? and role = 'member';");
            pst.setString(1, groupId);
            rs = pst.executeQuery();
            while(rs.next()){
                username = rs.getString("username");
                if(!currentUsername.equals(username)) {
                    model.addElement(username);
                } else {
                    model.addElement("You");
                }
            }
        } catch(HeadlessException | SQLException ex){
            System.out.println(ex);
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return model;
    }

    public String getLastPostDate(String groupId) {
        String lastPostDate = new String();
        try{
            con = dataBaseConnector.connect();
            String sql = "SELECT `groupnotes_group`.last_edit_datetime " +
                    "FROM noteworthy.`groupnotes_group` " +
                    "WHERE `groupnotes_group`.groupId = ? " +
                    "ORDER BY `groupnotes_group`.last_edit_datetime DESC " +
                    "LIMIT 1;";
            pst = con.prepareStatement(sql);
            pst.setString(1, groupId);
            rs = pst.executeQuery();
            if(rs.next()) {
                lastPostDate = rs.getString("last_edit_datetime");
            } else {
                pst = con.prepareStatement("select dateJoined from `user_group` where groupId = ? and role = 'creator';");
                pst.setString(1, groupId);
                rs = pst.executeQuery();
                rs.next();
                lastPostDate = rs.getString("dateJoined");
            }
        } catch(HeadlessException | SQLException ex){
            System.out.println(ex);
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return lastPostDate;
    }

    public String getRole(String currentUsername, String groupId) {
        String role = new String();
        try{
            con = dataBaseConnector.connect();
            pst = con.prepareStatement("select role from `user_group` where groupId = ? and username = ?;");
            pst.setString(1, groupId);
            pst.setString(2, currentUsername);
            rs = pst.executeQuery();
            rs.next();
            role = rs.getString("role");
        } catch(HeadlessException | SQLException ex){
            System.out.println(ex);
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return role;
    }

    public boolean deleteGroup(String groupId) {
        try{
            con = dataBaseConnector.connect();
            try {
                pst = con.prepareStatement("delete from `group` where groupId = ?;");
                pst.setString(1, groupId);
                pst.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e);
                return false;
            }
        } catch(HeadlessException ex){
            System.out.println(ex);
            return false;
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public Boolean LeaveGroup(String currentUsername, String groupId) {
        try{
            con = dataBaseConnector.connect();
            try {
                pst = con.prepareStatement("delete from `user_group` where username = ? and groupId = ?;");
                pst.setString(1, currentUsername);
                pst.setString(2, groupId);
                pst.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e);
                return false;
            }
        } catch(HeadlessException ex){
            System.out.println(ex);
            return false;
        }finally {
            try {
                if(con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
