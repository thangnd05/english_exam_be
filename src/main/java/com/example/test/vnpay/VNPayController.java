package com.example.test.vnpay;


import com.example.test.models.Users;
import com.example.test.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/vnpay")
public class VNPayController {

    @Autowired
    private UserService userService;

    // API để tạo URL thanh toán VNPay
    @PostMapping("/create-payment/{userId}")
    public ResponseEntity<?> createPayment(HttpServletRequest req, @PathVariable Long userId) throws UnsupportedEncodingException {
        // Kiểm tra user tồn tại
        Optional<Users> user = userService.getUserId(userId);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("Tài khoản không tồn tại");
        }

        Users userData=user.get();
        // Kiểm tra membershipId
        if (userData.getMembershipId() != 1) {
            return ResponseEntity.badRequest().body("Tài khoản đã có membership VIP");
        }

        // Cấu hình thông tin thanh toán
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = "Thanh toan VIP cho tai khoan voi id : " + userId;
        String vnp_OrderType = "service"; // Loại hàng hóa (tùy chỉnh)
        String vnp_TxnRef = Config.getRandomNumber(8); // Mã giao dịch ngẫu nhiên
        String vnp_IpAddr = Config.getIpAddress(req);
        String vnp_Locale = "vn";
        String vnp_CurrCode = "VND";

        long amount = 100000 * 100; // Số tiền thanh toán (100,000 VND, đơn vị: đồng)

        // Tạo tham số thanh toán
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", Config.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", vnp_Locale);
        vnp_Params.put("vnp_ReturnUrl", Config.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Thời gian tạo giao dịch
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Tạo secure hash sử dụng Config
        String vnp_SecureHash = Config.hashAllFields(vnp_Params);
        vnp_Params.put("vnp_SecureHash", vnp_SecureHash);

        // Tạo URL thanh toán
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()))
                    .append("&");
        }
        String paymentUrl = Config.vnp_PayUrl + "?" + query.substring(0, query.length() - 1);
        return ResponseEntity.ok(paymentUrl);
    }

    // API xử lý callback từ VNPay
    @GetMapping("/payment-callback")
    public ResponseEntity<?> paymentReturn(HttpServletRequest request) {
        Map<String, String> vnp_Params = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String paramName = params.nextElement();
            vnp_Params.put(paramName, request.getParameter(paramName));
        }

        // Kiểm tra tính hợp lệ của giao dịch
        String vnp_SecureHash = vnp_Params.get("vnp_SecureHash");
        vnp_Params.remove("vnp_SecureHash");
        String signData = Config.hashAllFields(vnp_Params);

        if (vnp_SecureHash.equals(signData)) {
            String userIdStr = vnp_Params.get("vnp_OrderInfo").split(": ")[1];
            Long userId = Long.parseLong(userIdStr);
            String vnp_ResponseCode = vnp_Params.get("vnp_ResponseCode");

            if ("00".equals(vnp_ResponseCode)) {
                // Thanh toán thành công, cập nhật membershipId
                userService.approvedMembership(userId);
                return ResponseEntity.ok("Thanh toán thành công, đã nâng cấp lên VIP");
            } else {
                return ResponseEntity.badRequest().body("Thanh toán thất bại: " + vnp_ResponseCode);
            }
        } else {
            return ResponseEntity.badRequest().body("Chữ ký không hợp lệ");
        }
    }
}