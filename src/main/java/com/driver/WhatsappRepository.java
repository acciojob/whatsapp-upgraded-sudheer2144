package com.driver;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Repository
public class WhatsappRepository {

    HashMap<String,User> userDB=new HashMap<>();
    HashMap<Group,List<User>> groupDB=new HashMap<>();
    HashMap<Integer,Message> messageDB=new HashMap<>();
    HashMap<String,List<Message>> userMessageDB=new HashMap<>();
    HashMap<Group,List<Message>> groupMessageDB=new HashMap<>();



    public String createUser(String name, String mobile) {
        if(userDB.containsKey(mobile)){
            throw new RuntimeException("User already exists");
        }
        userDB.put(mobile,new User(name,mobile));
        return "SUCCESS";
    }

    public int createMessage(String content){
        int id=messageDB.values().size()+1;
        Message message=new Message();
        message.setId(id);
        message.setContent(content);
        message.setTimestamp(new Date());
        messageDB.put(id,message);
        return id;
    }

    public Group createGroup(List<User> users){
        Group group=new Group();
        if(users.size()==2){
            group.setName(users.get(1).getName());
        }
        else if(users.size()>2){
            int count = 1;
            for(Group groupInDB: groupDB.keySet()){
                String groupName = groupInDB.getName();
                if(groupName.substring(0,5).equals("Group")){
                    count++;
                }
            }
            group.setName("Group "+String.valueOf(count));
            group.setNumberOfParticipants(users.size());
        }
        groupDB.put(group,users);
        return group;
    }

    public int sendMessage(Message message,User sender,Group group) throws Exception{

        Group groupFound=groupFinder(group);
        if(groupFound==null){
            throw new RuntimeException("Group does not exist");
        }

        List<User> groupUsersList=groupDB.get(groupFound);

        boolean userFound=userFinder(groupUsersList,sender);
        if(!userFound){
            throw new RuntimeException("You are not allowed to send message");
        }

        List<Message> userMessages=new ArrayList<>();
        if(userMessageDB.containsKey(sender.getMobile())){
            userMessages=userMessageDB.get(sender.getMobile());
        }
        userMessages.add(message);
        userMessageDB.put(sender.getMobile(),userMessages);

        List<Message> groupMessages=new ArrayList<>();
        if(groupMessageDB.containsKey(groupFound)){
            groupMessages=groupMessageDB.get(groupFound);
        }
        groupMessages.add(message);
        groupMessageDB.put(groupFound,groupMessages);

        return groupMessages.size();
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{

        Group foundGroup=groupFinder(group);
        if(foundGroup==null){
            throw new RuntimeException("Group does not exist");
        }

        List<User> groupUsers=groupDB.get(foundGroup);

        if(!groupUsers.get(0).getMobile().equals(user.getMobile())){
            throw new RuntimeException("Approver does not have rights");
        }

        boolean userFound=userFinder(groupUsers,user);
        if(!userFound){
            throw new RuntimeException("User is not a participant");
        }

        groupDB.remove(foundGroup);
        if(groupUsers.size()==2){
            foundGroup.setName(user.getName());
        }

        groupUsers.remove(user);
        groupUsers.add(0,user);

        groupDB.put(foundGroup,groupUsers);

        return "SUCCESS";
    }


    public Group groupFinder(Group group){
        Group foundGroup=null;
        for(Group groupInDB:groupDB.keySet()){
            if(groupInDB.getName().equals(group.getName())){
                foundGroup=groupInDB;
                break;
            }
        }
        return foundGroup;
    }

    public boolean userFinder(List<User> groupUsers,User user){
        for(User everyUser:groupUsers){
            if(everyUser.getMobile().equals(user.getMobile())){
                return true;
            }
        }
        return false;
    }


    public int removeUser(User user) {

        List<User> usersInGroupList=null;

        Group userGroup=null;

        for(Group group:groupDB.keySet()){
            List<User> usersList=groupDB.get(group);
            if(userFinder(usersList,user)){
                usersInGroupList=usersList;
                userGroup=group;
                break;
            }
        }
        if(usersInGroupList==null){
            throw new RuntimeException("User not found");
        }

        if(usersInGroupList.get(0).getMobile().equals(user.getMobile())){
            throw new RuntimeException("Cannot remove admin");
        }

        for(User userInList:usersInGroupList){
            if(user.getMobile().equals(userInList.getMobile())){
                usersInGroupList.remove(userInList);
                break;
            }
        }

        List<Message> userMessages=userMessageDB.get(user.getMobile());
        userMessageDB.remove(user.getMobile());

        List<Message> groupMessages=groupMessageDB.get(userGroup);

        for(Message userMessage:userMessages){
            groupMessages.remove(userMessage);
            messageDB.remove(userMessage.getId());
        }

        groupDB.put(userGroup,usersInGroupList);
        groupMessageDB.put(userGroup,groupMessages);

        return usersInGroupList.size()+groupMessages.size()+messageDB.size();
    }

    public String findMessage(Date start, Date end, int k) throws Exception{
        List<Message> messageList=new ArrayList<>();
        for(Message message: messageDB.values()){
            if(message.getTimestamp().after(start) && message.getTimestamp().before(end)){
                messageList.add(message);
            }
        }
        if(messageList.size()<k+1){
            throw new RuntimeException("K is greater than the number of messages");
        }
        return messageList.get(k).getContent();
    }
}
