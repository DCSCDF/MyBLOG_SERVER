/*
 * [RsaUtils.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/19 12:57
 */

package com.jiuliu.myblog_dev.config.rsa;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class RsaUtils {

    private static final Logger log = LoggerFactory.getLogger(RsaUtils.class);

    /**
     * 使用私钥解密 Base64 编码的 RSA 密文
     *
     * @param encryptedBase64  Base64 编码的密文（来自前端）
     * @param privateKeyBase64 Base64 编码的 PKCS#8 私钥
     * @return 解密后的明文
     * @throws DecryptionException 当解密失败时（如数据损坏、非密文、密钥不匹配等）
     */
    public static String decryptByPrivateKey(String encryptedBase64, String privateKeyBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            log.warn("RSA 解密失败：输入密文为空");
            throw new DecryptionException("无效的加密数据");
        }

        try {
            // 解码私钥
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // 解码并解密
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            String result = new String(decryptedBytes, StandardCharsets.UTF_8);
            log.debug("RSA 解密成功，明文长度: {}", result.length());
            return result;

        } catch (IllegalArgumentException e) {
            // Base64 解码失败
            log.warn("RSA 解密失败：输入数据不是有效的 Base64");
            throw new DecryptionException("加密数据格式错误");
        } catch (Exception e) {
            // 包括 BadPaddingException, IllegalBlockSizeException, InvalidKeyException 等
            log.warn("RSA 解密失败：{}", e.getMessage());
            // 不记录完整堆栈，避免日志膨胀和潜在信息泄露
            throw new DecryptionException("解密失败，请刷新页面重试");
        }
    }
}