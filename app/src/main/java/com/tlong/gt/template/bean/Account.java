package com.tlong.gt.template.bean;

import com.litesuits.orm.db.annotation.NotNull;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

/**
 * 账户
 * Created by GT on 2017/3/14.
 */
@Table("account")
public class Account {

    // 自增主键
    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private int id;

    // 非空字段
    @NotNull
    private String label;
    @NotNull
    private String name;
    @NotNull
    private String password;

    public Account() {}

    public Account(String label, String name, String password) {
        this.label = label;
        this.name = name;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
