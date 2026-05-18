package com.example.demo.service.notification;

import com.example.demo.repository.notification.entity.Notification;
import com.example.demo.repository.notification.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    // 通知类型常量
    public static final int TYPE_SYSTEM = 1;      // 系统通知
    public static final int TYPE_CONTRACT = 4;    // 合同通知
    public static final int TYPE_PAYMENT = 5;     // 支付通知
    public static final int TYPE_ISSUE = 6;       // 问题反馈

    /**
     * 发送通知
     */
    public void send(Long userId, int notifyType, String title, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setNotifyType(notifyType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    /**
     * 发送通知（无关联ID）
     */
    public void send(Long userId, int notifyType, String title, String content) {
        send(userId, notifyType, title, content, null);
    }

    // ==================== 合同相关通知 ====================

    public void notifyContractSent(Long tenantId, String houseTitle, Long contractId) {
        send(tenantId, TYPE_CONTRACT, "合同待确认",
                "房东已上传【" + houseTitle + "】的租赁合同，请查看并确认是否与线下签署的一致", contractId);
    }

    public void notifyContractSigned(Long landlordId, String tenantName, String houseTitle, Long contractId) {
        send(landlordId, TYPE_CONTRACT, "租客已确认合同",
                "租客 " + tenantName + " 已确认【" + houseTitle + "】的租赁合同", contractId);
    }

    public void notifyContractEffective(Long tenantId, Long landlordId, String houseTitle, Long contractId) {
        send(tenantId, TYPE_CONTRACT, "合同已确认",
                "【" + houseTitle + "】的租赁合同已确认，请及时支付首期租金", contractId);
        send(landlordId, TYPE_CONTRACT, "合同已确认",
                "租客已确认【" + houseTitle + "】的租赁合同，等待租客支付首期租金", contractId);
    }

    public void notifyNegotiateCreated(Long landlordId, String tenantName, String houseTitle, Long contractId) {
        send(landlordId, TYPE_CONTRACT, "租客提出修改意见",
                "租客 " + tenantName + " 对【" + houseTitle + "】的合同提出修改意见，请处理", contractId);
    }

    // ==================== 支付相关通知 ====================

    public void notifyPaymentCreated(Long tenantId, String houseTitle, String amount, Long orderId) {
        send(tenantId, TYPE_PAYMENT, "待支付订单",
                "【" + houseTitle + "】有新的待支付订单，金额：¥" + amount, orderId);
    }

    public void notifyPaymentSuccess(Long landlordId, String tenantName, String houseTitle, String amount, Long orderId) {
        send(landlordId, TYPE_PAYMENT, "收到租金",
                "租客 " + tenantName + " 已支付【" + houseTitle + "】租金 ¥" + amount, orderId);
    }

    public void notifyPaymentOverdue(Long tenantId, String houseTitle, Long orderId) {
        send(tenantId, TYPE_PAYMENT, "租金逾期提醒",
                "【" + houseTitle + "】的租金已逾期，请尽快支付", orderId);
    }

    public void notifyDepositRefundApplied(Long landlordId, String tenantName, String houseTitle, Long orderId) {
        send(landlordId, TYPE_PAYMENT, "押金退还申请",
                "租客 " + tenantName + " 申请退还【" + houseTitle + "】的押金，请处理", orderId);
    }

    public void notifyDepositRefunded(Long tenantId, String houseTitle, String amount, Long orderId) {
        send(tenantId, TYPE_PAYMENT, "押金已退还",
                "【" + houseTitle + "】的押金 ¥" + amount + " 已退还", orderId);
    }

    // ==================== 问题反馈通知 ====================

    public void notifyIssueCreated(Long landlordId, String tenantName, String houseTitle, Long manageId) {
        send(landlordId, TYPE_ISSUE, "新的问题反馈",
                "租客 " + tenantName + " 提交了【" + houseTitle + "】的问题反馈，请处理", manageId);
    }

    public void notifyIssueProcessed(Long tenantId, String houseTitle, Long manageId) {
        send(tenantId, TYPE_ISSUE, "问题处理进度更新",
                "您提交的【" + houseTitle + "】问题反馈有新的处理进度", manageId);
    }

    public void notifyIssueResolved(Long tenantId, String houseTitle, Long manageId) {
        send(tenantId, TYPE_ISSUE, "问题已解决",
                "您提交的【" + houseTitle + "】问题反馈已解决", manageId);
    }

    // ==================== 系统通知 ====================

    public void notifyRealnameApproved(Long userId) {
        send(userId, TYPE_SYSTEM, "实名认证通过",
                "恭喜！您的实名认证已通过审核，现在可以使用完整功能了");
    }

    public void notifyRealnameRejected(Long userId, String reason) {
        send(userId, TYPE_SYSTEM, "实名认证未通过",
                "您的实名认证未通过审核，原因：" + (reason != null ? reason : "信息不符") + "，请重新提交");
    }

    public void notifyHouseApproved(Long landlordId, String houseTitle, Long houseId) {
        send(landlordId, TYPE_SYSTEM, "房源审核通过",
                "您发布的房源【" + houseTitle + "】已通过审核，现已上架展示", houseId);
    }

    public void notifyHouseRejected(Long landlordId, String houseTitle, String reason, Long houseId) {
        send(landlordId, TYPE_SYSTEM, "房源审核未通过",
                "您发布的房源【" + houseTitle + "】未通过审核，原因：" + reason, houseId);
    }

    public void notifyHouseOffline(Long landlordId, String houseTitle, String reason, Long houseId) {
        send(landlordId, TYPE_SYSTEM, "房源已被下架",
                "您的房源【" + houseTitle + "】已被管理员下架，原因：" + reason, houseId);
    }

    // ==================== 退租通知 ====================

    public void notifyCheckoutApply(Long landlordId, String tenantName, String houseTitle, Long manageId) {
        send(landlordId, TYPE_CONTRACT, "收到退租申请",
                "租客 " + tenantName + " 申请退租【" + houseTitle + "】，请及时处理", manageId);
    }

    public void notifyCheckoutApproved(Long tenantId, String houseTitle, Long manageId) {
        send(tenantId, TYPE_CONTRACT, "退租申请已通过",
                "您的【" + houseTitle + "】退租申请已通过，请等待房东安排交接", manageId);
    }

    public void notifyCheckoutRejected(Long tenantId, String houseTitle, String reason, Long manageId) {
        send(tenantId, TYPE_CONTRACT, "退租申请被拒绝",
                "您的【" + houseTitle + "】退租申请被拒绝，原因：" + (reason != null ? reason : "房东拒绝"), manageId);
    }

    public void notifyHandoverArranged(Long tenantId, String houseTitle, String handoverTime, Long manageId) {
        send(tenantId, TYPE_CONTRACT, "交接时间已安排",
                "【" + houseTitle + "】的退租交接时间已安排：" + (handoverTime != null ? handoverTime : "待定") + "，请按时交接", manageId);
    }

    public void notifyCheckoutComplete(Long userId, String houseTitle, Long manageId) {
        send(userId, TYPE_CONTRACT, "退租已完成",
                "【" + houseTitle + "】的退租流程已完成，合同已终止", manageId);
    }

    // ==================== 续租通知 ====================

    public void notifyRenewalApplied(Long landlordId, String tenantName, String houseTitle, Long contractId) {
        send(landlordId, TYPE_CONTRACT, "收到续租申请",
                "租客 " + tenantName + " 申请续租【" + houseTitle + "】，请及时处理", contractId);
    }

    public void notifyRenewalApproved(Long tenantId, String houseTitle, Long newContractId) {
        send(tenantId, TYPE_CONTRACT, "续租申请已同意",
                "房东已同意您的【" + houseTitle + "】续租申请，并上传了新合同，请查看确认", newContractId);
    }

    public void notifyRenewalRejected(Long tenantId, String houseTitle, String reason, Long contractId) {
        send(tenantId, TYPE_CONTRACT, "续租申请被拒绝",
                "您的【" + houseTitle + "】续租申请被拒绝" + (reason != null && !reason.isEmpty() ? "，原因：" + reason : ""), contractId);
    }

    // ==================== 租金提醒通知 ====================

    public void notifyRentDue(Long tenantId, String houseTitle, String dueDate, Long orderId) {
        send(tenantId, TYPE_PAYMENT, "租金缴纳提醒",
                "【" + houseTitle + "】的租金将于 " + dueDate + " 到期，请及时缴纳", orderId);
    }

    public void notifyContractExpiring(Long tenantId, Long landlordId, String houseTitle, String expireDate, Long contractId) {
        send(tenantId, TYPE_CONTRACT, "合同即将到期",
                "【" + houseTitle + "】的租赁合同将于 " + expireDate + " 到期，请注意续约或退租事宜", contractId);
        send(landlordId, TYPE_CONTRACT, "合同即将到期",
                "【" + houseTitle + "】的租赁合同将于 " + expireDate + " 到期", contractId);
    }
}
