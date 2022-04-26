package com.atguigu.gmall.pms.service.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StreamTest {
    public static void main(String[] args) {
        List<User> users = Arrays.asList(
                new User("sam", 10),
                new User("dawn", 12),
                new User("jack", 15),
                new User("tom", 20)
        );

//        users.stream().map(user ->  user.getUsername()).collect(Collectors.toList()).forEach(System.out::println);
//          users.stream().map(User::getUsername).collect(Collectors.toList()).forEach(System.out::println);
//        users.stream().map(my ->  my.getUsername()).collect(Collectors.toList()).forEach(System.out::println);
//        users.stream().map(user -> {
//           return user.getUsername();
//        }).collect(Collectors.toList()).forEach(System.out::println);

//        users.stream().filter(user -> user.getAge() >= 15).collect(Collectors.toList()).forEach(System.out::println);
//        users.stream().filter(user -> {
//            boolean flag = user.getAge() >= 15;
//            return flag;
//        }).collect(Collectors.toList()).forEach(System.out::println);

//        System.out.println(users.stream().map(User::getAge).reduce((x, u) -> x + u).get());

    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class User {
    private String username;
    private Integer age;
}