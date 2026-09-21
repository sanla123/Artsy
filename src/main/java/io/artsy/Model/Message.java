package io.artsy.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private UserTbl sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private UserTbl receiver;

    @ManyToOne
    @JoinColumn(name = "commission_id")
    private Commission commission;

    private String content;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String image;

    private LocalDateTime sentAt;

}