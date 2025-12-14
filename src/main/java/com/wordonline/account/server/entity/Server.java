package com.wordonline.account.server.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "servers")
public class Server {

    @Id
    private Long id;

    private String protocol;
    private String domain;
    private Integer port;
    private ServerType type;

    @Setter
    private ServerState state;

    public String getUrl() {
        return String.format("%s://%s:%d", protocol, domain, port);
    }

    public Server(String protocol, String domain, int port, ServerType serverType, ServerState state) {
        this(null, protocol, domain, port, serverType, state);
    }
}

