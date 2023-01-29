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
        int id=messageDB.size()+1;
        Message message=new Message();
        message.setId(id);
        message.setContent(content);
        message.setTimestamp(new Date());
        messageDB.put(id,message);
        return id;
    }

    public Group createGroup(List<User> users){
        Group newGroup=new Group();
        if(users.size()==2){
            newGroup.setName(users.get(1).getName());
            newGroup.setNumberOfParticipants(users.size());
        }
        else{
            int count = 1;
            for(Group groupInDB: groupDB.keySet()){
                if(groupDB.get(groupInDB).size()>2){
                    count++;
                }
            }
            newGroup.setName("Group "+String.valueOf(count));
            newGroup.setNumberOfParticipants(users.size());
        }
        groupDB.put(newGroup,users);
        return newGroup;
    }

    public int sendMessage(Message message,User sender,Group group) throws Exception{

        Group groupFound=groupFinder(group);
        if(groupFound==null){
            throw new RuntimeException("Group does not exist");
        }

        List<User> groupUsersList=groupDB.get(groupFound);

        User userFound=userFinder(groupUsersList,sender);
        if(userFound==null){
            throw new RuntimeException("You are not allowed to send message");
        }

        message.setId(messageDB.size()+1);
        messageDB.put(message.getId(),message);

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

        if(!groupUsers.get(0).getMobile().equals(approver.getMobile())){
            throw new RuntimeException("Approver does not have rights");
        }

        User userFound=userFinder(groupUsers,user);

        if(userFound==null){
            throw new RuntimeException("User is not a participant");
        }

        groupUsers.remove(userFound);
        groupUsers.add(0,userFound);

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

    public User userFinder(List<User> groupUsers,User user){
        for(User everyUser:groupUsers){
            if(everyUser.getMobile().equals(user.getMobile())){
                return everyUser;
            }
        }
        return null;
    }


    public int removeUser(User user) {

        List<User> usersInGroupList=null;

        Group userGroup=null;

        User userInGroup=null;

        for(Group group:groupDB.keySet()){
            List<User> usersList=groupDB.get(group);
            User findUser=userFinder(usersList,user);
            if(findUser!=null){
                usersInGroupList=usersList;
                userGroup=group;
                userInGroup=findUser;
                break;
            }
        }

        if(userInGroup==null){
            throw new RuntimeException("User not found");
        }

        if(usersInGroupList.get(0).getMobile().equals(userInGroup.getMobile())){
            throw new RuntimeException("Cannot remove admin");
        }

        usersInGroupList.remove(userInGroup);

        List<Message> userMessages=userMessageDB.get(userInGroup.getMobile());
        userMessageDB.remove(userInGroup.getMobile());

        List<Message> groupMessages=groupMessageDB.get(userGroup);

        for(Message userMessage:userMessages){
            groupMessages.remove(userMessage);
            messageDB.remove(userMessage.getId());
        }

        if(usersInGroupList.size()<=2){
            userGroup.setNumberOfParticipants(2);
            userGroup.setName(usersInGroupList.get(usersInGroupList.size()-1).getName());
        }

        groupDB.put(userGroup,usersInGroupList);
        groupMessageDB.put(userGroup,groupMessages);

        int countMessagesInAllGroups=0;
        for(List<Message> messages:groupMessageDB.values()){
            countMessagesInAllGroups+=messages.size();
        }
        return groupDB.get(userGroup).size()+groupMessageDB.get(userGroup).size()+messageDB.size();
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
