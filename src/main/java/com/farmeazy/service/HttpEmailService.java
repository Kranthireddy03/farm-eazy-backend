package com.farmeazy.service;

import com.farmeazy.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP-based Email Service using Resend API
 * 
 * This service uses Resend's REST API instead of SMTP, which works
 * on platforms that block outbound SMTP connections (like Render free tier).
 * 
 * Features:
 * - Fast HTTP-based email delivery (no SMTP timeouts)
 * - Async methods for non-blocking operations
 * - 10-second timeout to prevent hanging
 * 
 * Setup:
 * 1. Sign up at https://resend.com
 * 2. Get your API key from the dashboard
 * 3. Set RESEND_API_KEY environment variable in Render
 * 
 * @author FarmEazy Development Team
 */
@Service
public class HttpEmailService {
    /**
     * Send a professional notification email (order failed, payment failed, etc)
     */
    public boolean sendNotificationEmail(String userEmail, String userName, String subject, String message) {
        String html = buildNotificationEmailHtml(userName, subject, message);
        return sendEmail(userEmail, subject, html);
    }

    // --- STUBS FOR MISSING NOTIFICATION METHODS ---
    public boolean sendProductUpdateConfirmation(String userEmail, String userName, String productName, String category, Double price, Integer quantity, String unit) {
        String subject = "Product Updated Successfully - " + productName;
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 650px; margin: 0 auto; background: #ffffff; }
                    .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; }
                    .header p { margin: 10px 0 0 0; opacity: 0.9; font-size: 14px; }
                    .content { padding: 40px 30px; background: #fafafa; }
                    .greeting { font-size: 18px; color: #111827; margin-bottom: 20px; }
                    .message { color: #4b5563; margin-bottom: 30px; line-height: 1.8; }
                    .details-card { background: white; border-radius: 12px; padding: 25px; margin: 25px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                    .card-title { font-size: 20px; font-weight: 700; color: #f59e0b; margin: 0 0 20px 0; border-bottom: 2px solid #f59e0b; padding-bottom: 10px; }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 12px 0; border-bottom: 1px solid #e5e7eb; }
                    td:first-child { color: #6b7280; font-weight: 500; width: 40%%; }
                    td:last-child { color: #111827; font-weight: 600; text-align: right; }
                    tr:last-child td { border-bottom: none; }
                    .price-highlight { font-size: 24px; color: #f59e0b; font-weight: 700; }
                    .button { display: inline-block; background: linear-gradient(135deg, #f59e0b, #d97706); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 16px; box-shadow: 0 4px 6px rgba(245, 158, 11, 0.25); }
                    .button:hover { background: linear-gradient(135deg, #d97706, #b45309); }
                    .info-box { background: #dbeafe; border-left: 4px solid #3b82f6; padding: 16px; border-radius: 6px; margin: 25px 0; }
                    .info-box p { margin: 0; color: #1e40af; font-size: 14px; }
                    .footer { background: #111827; color: #9ca3af; padding: 30px; text-align: center; font-size: 14px; }
                    .footer a { color: #f59e0b; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✏️ Product Updated</h1>
                        <p>Your listing has been successfully updated</p>
                    </div>
                    <div class="content">
                        <div class="greeting">Hello %s! 👋</div>
                        <div class="message">
                            Your product listing has been successfully updated on FarmEazy marketplace. Buyers can now see your latest changes!
                        </div>

                        <div class="details-card">
                            <h2 class="card-title">📦 Updated Product Details</h2>
                            <table>
                                <tr>
                                    <td>Product Name</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Category</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Price per Unit</td>
                                    <td class="price-highlight">₹%s</td>
                                </tr>
                                <tr>
                                    <td>Available Quantity</td>
                                    <td>%d %s</td>
                                </tr>
                            </table>
                        </div>

                        <div class="info-box">
                            <p><strong>💡 Tip:</strong> Keep your product details updated to attract more buyers!</p>
                        </div>

                        <center>
                            <a href="%s/selling" class="button">View My Listings</a>
                        </center>

                        <div class="message" style="margin-top: 30px; font-size: 14px; color: #6b7280;">
                            Your product is now live on the marketplace. Buyers searching for "%s" will find your listing.
                        </div>
                    </div>
                    <div class="footer">
                        <p><strong>FarmEazy</strong> - Smart Farm Management</p>
                        <p style="margin-top: 10px;">
                            <a href="%s">Visit Website</a> |
                            <a href="%s/support">Support</a>
                        </p>
                        <p style="margin-top: 15px; font-size: 12px;">© 2026 FarmEazy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, productName, category, price, quantity, unit, appBaseUrl, category, appBaseUrl, appBaseUrl);
        return sendEmail(userEmail, subject, html);
    }

    public boolean sendProductDeleteConfirmation(String userEmail, String userName, String productName, String category, Double price, Integer quantity, String unit) {
        String subject = "Product Removed - " + productName;
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 650px; margin: 0 auto; background: #ffffff; }
                    .header { background: linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; }
                    .header p { margin: 10px 0 0 0; opacity: 0.9; font-size: 14px; }
                    .content { padding: 40px 30px; background: #fafafa; }
                    .greeting { font-size: 18px; color: #111827; margin-bottom: 20px; }
                    .message { color: #4b5563; margin-bottom: 30px; line-height: 1.8; }
                    .details-card { background: white; border-radius: 12px; padding: 25px; margin: 25px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-left: 4px solid #ef4444; }
                    .card-title { font-size: 20px; font-weight: 700; color: #ef4444; margin: 0 0 20px 0; }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 12px 0; border-bottom: 1px solid #e5e7eb; }
                    td:first-child { color: #6b7280; font-weight: 500; width: 40%%; }
                    td:last-child { color: #111827; font-weight: 600; text-align: right; }
                    tr:last-child td { border-bottom: none; }
                    .button { display: inline-block; background: linear-gradient(135deg, #22c55e, #16a34a); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 16px; }
                    .footer { background: #111827; color: #9ca3af; padding: 30px; text-align: center; font-size: 14px; }
                    .footer a { color: #f59e0b; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🗑️ Product Removed</h1>
                        <p>Your listing has been removed from marketplace</p>
                    </div>
                    <div class="content">
                        <div class="greeting">Hello %s,</div>
                        <div class="message">
                            Your product has been successfully removed from FarmEazy marketplace. This product is no longer visible to buyers.
                        </div>

                        <div class="details-card">
                            <h2 class="card-title">📦 Removed Product Details</h2>
                            <table>
                                <tr>
                                    <td>Product Name</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Category</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Price per Unit</td>
                                    <td>₹%s</td>
                                </tr>
                                <tr>
                                    <td>Quantity</td>
                                    <td>%d %s</td>
                                </tr>
                            </table>
                        </div>

                        <div class="message" style="font-size: 14px; color: #6b7280;">
                            If you removed this product by mistake, you can list it again from your seller dashboard.
                        </div>

                        <center>
                            <a href="%s/selling" class="button">Add New Product</a>
                        </center>
                    </div>
                    <div class="footer">
                        <p><strong>FarmEazy</strong> - Smart Farm Management</p>
                        <p style="margin-top: 10px;">
                            <a href="%s">Visit Website</a> |
                            <a href="%s/support">Support</a>
                        </p>
                        <p style="margin-top: 15px; font-size: 12px;">© 2026 FarmEazy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, productName, category, price, quantity, unit, appBaseUrl, appBaseUrl, appBaseUrl);
        return sendEmail(userEmail, subject, html);
    }

    public boolean sendCoinEarnedNotification(String userEmail, String userName, Integer amount, Integer totalCoins, String reason) {
        String subject = "🪙 Coins Earned - FarmEazy";
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 650px; margin: 0 auto; background: #ffffff; }
                    .header { background: linear-gradient(135deg, #fbbf24 0%%, #f59e0b 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; }
                    .coin-display { font-size: 60px; margin: 20px 0; }
                    .content { padding: 40px 30px; background: #fafafa; }
                    .greeting { font-size: 18px; color: #111827; margin-bottom: 20px; }
                    .details-card { background: white; border-radius: 12px; padding: 25px; margin: 25px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 12px 0; border-bottom: 1px solid #e5e7eb; }
                    td:first-child { color: #6b7280; font-weight: 500; width: 50%%; }
                    td:last-child { color: #111827; font-weight: 700; text-align: right; font-size: 18px; }
                    tr:last-child td { border-bottom: none; }
                    .total-row td { color: #f59e0b; font-size: 24px; padding-top: 20px; border-top: 2px solid #f59e0b; }
                    .button { display: inline-block; background: linear-gradient(135deg, #fbbf24, #f59e0b); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 16px; }
                    .footer { background: #111827; color: #9ca3af; padding: 30px; text-align: center; font-size: 14px; }
                    .footer a { color: #fbbf24; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Congratulations!</h1>
                        <div class="coin-display">🪙</div>
                        <p style="font-size: 18px; margin: 0;">You've earned %d coins!</p>
                    </div>
                    <div class="content">
                        <div class="greeting">Hello %s! 👋</div>

                        <div class="details-card">
                            <table>
                                <tr>
                                    <td>Coins Earned</td>
                                    <td>+%d 🪙</td>
                                </tr>
                                <tr>
                                    <td>Reason</td>
                                    <td>%s</td>
                                </tr>
                                <tr class="total-row">
                                    <td>Total Balance</td>
                                    <td>%d 🪙</td>
                                </tr>
                            </table>
                        </div>

                        <div style="background: #dbeafe; border-left: 4px solid #3b82f6; padding: 16px; border-radius: 6px; margin: 25px 0;">
                            <p style="margin: 0; color: #1e40af; font-size: 14px;">
                                <strong>💡 Use your coins:</strong> Apply coins as discount on your next order. 1 coin = ₹1 discount!
                            </p>
                        </div>

                        <center>
                            <a href="%s/buying" class="button">Start Shopping</a>
                        </center>
                    </div>
                    <div class="footer">
                        <p><strong>FarmEazy</strong> - Smart Farm Management</p>
                        <p style="margin-top: 10px;">
                            <a href="%s">Visit Website</a> |
                            <a href="%s/activities">View Activity</a>
                        </p>
                        <p style="margin-top: 15px; font-size: 12px;">© 2026 FarmEazy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, amount, userName, amount, reason, totalCoins, appBaseUrl, appBaseUrl, appBaseUrl);
        return sendEmail(userEmail, subject, html);
    }

    public boolean sendCoinSpentNotification(String userEmail, String userName, Integer amount, Integer totalCoins) {
        String subject = "Coins Used - FarmEazy";
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 650px; margin: 0 auto; background: #ffffff; }
                    .header { background: linear-gradient(135deg, #6366f1 0%%, #4f46e5 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; }
                    .content { padding: 40px 30px; background: #fafafa; }
                    .greeting { font-size: 18px; color: #111827; margin-bottom: 20px; }
                    .details-card { background: white; border-radius: 12px; padding: 25px; margin: 25px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 12px 0; border-bottom: 1px solid #e5e7eb; }
                    td:first-child { color: #6b7280; font-weight: 500; width: 50%%; }
                    td:last-child { color: #111827; font-weight: 700; text-align: right; font-size: 18px; }
                    tr:last-child td { border-bottom: none; }
                    .total-row td { color: #6366f1; font-size: 24px; padding-top: 20px; border-top: 2px solid #6366f1; }
                    .button { display: inline-block; background: linear-gradient(135deg, #fbbf24, #f59e0b); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 16px; }
                    .footer { background: #111827; color: #9ca3af; padding: 30px; text-align: center; font-size: 14px; }
                    .footer a { color: #fbbf24; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>💳 Coins Redeemed</h1>
                        <p style="margin: 10px 0 0 0; font-size: 14px;">You saved money with your coins!</p>
                    </div>
                    <div class="content">
                        <div class="greeting">Hello %s! 👋</div>

                        <div class="details-card">
                            <table>
                                <tr>
                                    <td>Coins Used</td>
                                    <td>-%d 🪙</td>
                                </tr>
                                <tr>
                                    <td>Discount Applied</td>
                                    <td>₹%d</td>
                                </tr>
                                <tr class="total-row">
                                    <td>Remaining Balance</td>
                                    <td>%d 🪙</td>
                                </tr>
                            </table>
                        </div>

                        <div style="background: #d1fae5; border-left: 4px solid #10b981; padding: 16px; border-radius: 6px; margin: 25px 0;">
                            <p style="margin: 0; color: #065f46; font-size: 14px;">
                                <strong>💚 Great choice!</strong> Keep completing activities to earn more coins and save on future purchases!
                            </p>
                        </div>

                        <center>
                            <a href="%s/activities" class="button">Earn More Coins</a>
                        </center>
                    </div>
                    <div class="footer">
                        <p><strong>FarmEazy</strong> - Smart Farm Management</p>
                        <p style="margin-top: 10px;">
                            <a href="%s">Visit Website</a> |
                            <a href="%s/orders">My Orders</a>
                        </p>
                        <p style="margin-top: 15px; font-size: 12px;">© 2026 FarmEazy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, amount, amount, totalCoins, appBaseUrl, appBaseUrl, appBaseUrl);
        return sendEmail(userEmail, subject, html);
    }

    public boolean sendServiceListingCreatedNotification(String userEmail, String userName, String title, Double rate, String description) {
        String subject = "Service Listed Successfully - " + title;
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 650px; margin: 0 auto; background: #ffffff; }
                    .header { background: linear-gradient(135deg, #8b5cf6 0%%, #7c3aed 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; }
                    .header p { margin: 10px 0 0 0; opacity: 0.9; font-size: 14px; }
                    .content { padding: 40px 30px; background: #fafafa; }
                    .greeting { font-size: 18px; color: #111827; margin-bottom: 20px; }
                    .details-card { background: white; border-radius: 12px; padding: 25px; margin: 25px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                    .card-title { font-size: 20px; font-weight: 700; color: #8b5cf6; margin: 0 0 20px 0; border-bottom: 2px solid #8b5cf6; padding-bottom: 10px; }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 12px 0; border-bottom: 1px solid #e5e7eb; }
                    td:first-child { color: #6b7280; font-weight: 500; width: 40%%; }
                    td:last-child { color: #111827; font-weight: 600; text-align: right; }
                    tr:last-child td { border-bottom: none; }
                    .rate-highlight { font-size: 24px; color: #8b5cf6; font-weight: 700; }
                    .description-box { background: #f3f4f6; padding: 15px; border-radius: 8px; margin: 15px 0; }
                    .button { display: inline-block; background: linear-gradient(135deg, #8b5cf6, #7c3aed); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 16px; }
                    .info-box { background: #dbeafe; border-left: 4px solid #3b82f6; padding: 16px; border-radius: 6px; margin: 25px 0; }
                    .footer { background: #111827; color: #9ca3af; padding: 30px; text-align: center; font-size: 14px; }
                    .footer a { color: #8b5cf6; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚜 Service Listed!</h1>
                        <p>Your service is now live and ready for bookings</p>
                    </div>
                    <div class="content">
                        <div class="greeting">Hello %s! 👋</div>
                        <div style="color: #4b5563; margin-bottom: 30px; line-height: 1.8;">
                            Great news! Your irrigation service is now listed on FarmEazy. Farmers can now discover and book your service.
                        </div>

                        <div class="details-card">
                            <h2 class="card-title">🔧 Service Details</h2>
                            <table>
                                <tr>
                                    <td>Service Title</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Hourly Rate</td>
                                    <td class="rate-highlight">₹%s/hr</td>
                                </tr>
                            </table>
                            <div class="description-box">
                                <strong style="color: #6b7280;">Description:</strong>
                                <p style="margin: 8px 0 0 0; color: #111827;">%s</p>
                            </div>
                        </div>

                        <div class="info-box">
                            <p style="margin: 0; color: #1e40af; font-size: 14px;">
                                <strong>💡 Tips for more bookings:</strong> Respond quickly to booking requests, maintain competitive pricing, and keep your availability updated.
                            </p>
                        </div>

                        <center>
                            <a href="%s/irrigation-services" class="button">Manage My Services</a>
                        </center>
                    </div>
                    <div class="footer">
                        <p><strong>FarmEazy</strong> - Smart Farm Management</p>
                        <p style="margin-top: 10px;">
                            <a href="%s">Visit Website</a> |
                            <a href="%s/support">Support</a>
                        </p>
                        <p style="margin-top: 15px; font-size: 12px;">© 2026 FarmEazy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, title, rate, description, appBaseUrl, appBaseUrl, appBaseUrl);
        return sendEmail(userEmail, subject, html);
    }

    public boolean sendServiceListingUpdatedNotification(String userEmail, String userName, String title, Double rate, String description) {
        String subject = "Service Updated - " + title;
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 650px; margin: 0 auto; background: #ffffff; }
                    .header { background: linear-gradient(135deg, #3b82f6 0%%, #2563eb 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; }
                    .content { padding: 40px 30px; background: #fafafa; }
                    .details-card { background: white; border-radius: 12px; padding: 25px; margin: 25px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                    .card-title { font-size: 20px; font-weight: 700; color: #3b82f6; margin: 0 0 20px 0; border-bottom: 2px solid #3b82f6; padding-bottom: 10px; }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 12px 0; border-bottom: 1px solid #e5e7eb; }
                    td:first-child { color: #6b7280; font-weight: 500; width: 40%%; }
                    td:last-child { color: #111827; font-weight: 600; text-align: right; }
                    tr:last-child td { border-bottom: none; }
                    .button { display: inline-block; background: linear-gradient(135deg, #3b82f6, #2563eb); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 16px; }
                    .footer { background: #111827; color: #9ca3af; padding: 30px; text-align: center; font-size: 14px; }
                    .footer a { color: #3b82f6; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✏️ Service Updated</h1>
                    </div>
                    <div class="content">
                        <div style="font-size: 18px; color: #111827; margin-bottom: 20px;">Hello %s! 👋</div>

                        <div class="details-card">
                            <h2 class="card-title">🔧 Updated Service Details</h2>
                            <table>
                                <tr>
                                    <td>Service Title</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Hourly Rate</td>
                                    <td style="font-size: 24px; color: #3b82f6; font-weight: 700;">₹%s/hr</td>
                                </tr>
                            </table>
                        </div>

                        <center>
                            <a href="%s/irrigation-services" class="button">View Service</a>
                        </center>
                    </div>
                    <div class="footer">
                        <p><strong>FarmEazy</strong> - Smart Farm Management</p>
                        <p style="margin-top: 15px; font-size: 12px;">© 2026 FarmEazy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, title, rate, appBaseUrl);
        return sendEmail(userEmail, subject, html);
    }

    public boolean sendServiceListingDeletedNotification(String userEmail, String userName, String title, Double rate, String description) {
        String subject = "Service Removed - " + title;
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 650px; margin: 0 auto; background: #ffffff; }
                    .header { background: linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; }
                    .content { padding: 40px 30px; background: #fafafa; }
                    .details-card { background: white; border-radius: 12px; padding: 25px; margin: 25px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-left: 4px solid #ef4444; }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 12px 0; border-bottom: 1px solid #e5e7eb; }
                    td:first-child { color: #6b7280; font-weight: 500; width: 40%%; }
                    td:last-child { color: #111827; font-weight: 600; text-align: right; }
                    .button { display: inline-block; background: linear-gradient(135deg, #22c55e, #16a34a); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 16px; }
                    .footer { background: #111827; color: #9ca3af; padding: 30px; text-align: center; font-size: 14px; }
                    .footer a { color: #22c55e; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🗑️ Service Removed</h1>
                    </div>
                    <div class="content">
                        <div style="font-size: 18px; color: #111827; margin-bottom: 20px;">Hello %s,</div>

                        <div class="details-card">
                            <table>
                                <tr>
                                    <td>Service Title</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Rate</td>
                                    <td>₹%s/hr</td>
                                </tr>
                            </table>
                        </div>

                        <center>
                            <a href="%s/irrigation-services" class="button">Add New Service</a>
                        </center>
                    </div>
                    <div class="footer">
                        <p><strong>FarmEazy</strong> - Smart Farm Management</p>
                        <p style="margin-top: 15px; font-size: 12px;">© 2026 FarmEazy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, title, rate, appBaseUrl);
        return sendEmail(userEmail, subject, html);
    }

    public boolean sendServiceBookingApprovedNotification(String userEmail, String userName, String serviceType, String location, String providerName) {
        String subject = "Service Booking Approved - FarmEazy";
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #22c55e 0%%, #16a34a 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .booking-card { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #22c55e; }
                    .detail-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #e5e7eb; }
                    .label { font-weight: bold; color: #6b7280; }
                    .value { color: #111827; }
                    .success-badge { display: inline-block; background: #dcfce7; color: #16a34a; padding: 8px 16px; border-radius: 20px; font-weight: bold; margin: 20px 0; }
                    .action-button { display: inline-block; background: #22c55e; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; margin: 20px 0; font-weight: bold; }
                    .footer { text-align: center; color: #6b7280; font-size: 14px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Booking Approved!</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p>Great news! Your service booking request has been approved.</p>

                        <div class="booking-card">
                            <div class="success-badge">✓ APPROVED</div>
                            <div class="detail-row">
                                <span class="label">Service Type:</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">Location:</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">Provider:</span>
                                <span class="value">%s</span>
                            </div>
                        </div>

                        <p><strong>What's Next?</strong></p>
                        <ul>
                            <li>The provider will contact you shortly to coordinate the service</li>
                            <li>Make sure your equipment/farm is ready at the scheduled time</li>
                            <li>Keep your contact details updated in your profile</li>
                        </ul>

                        <a href="%s/irrigation-services" class="action-button">View My Bookings</a>

                        <p style="margin-top: 30px; color: #6b7280; font-size: 14px;">
                            If you have any questions or need to make changes, please log in to your account or contact the service provider directly.
                        </p>

                        <div class="footer">
                            <p>Thank you for using FarmEazy!</p>
                            <p><a href="%s" style="color: #22c55e;">www.farm-eazy.com</a></p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, userName, serviceType, location, providerName, appBaseUrl, appBaseUrl);

        return sendEmail(userEmail, subject, html);
    }

    public boolean sendServiceBookingDeclinedNotification(String userEmail, String userName, String serviceType, String location, String providerName) {
        String subject = "Service Booking Update - FarmEazy";
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .booking-card { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #f59e0b; }
                    .detail-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #e5e7eb; }
                    .label { font-weight: bold; color: #6b7280; }
                    .value { color: #111827; }
                    .warning-badge { display: inline-block; background: #fef3c7; color: #d97706; padding: 8px 16px; border-radius: 20px; font-weight: bold; margin: 20px 0; }
                    .action-button { display: inline-block; background: #22c55e; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; margin: 20px 0; font-weight: bold; }
                    .footer { text-align: center; color: #6b7280; font-size: 14px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Booking Update</h1>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p>We regret to inform you that your service booking request could not be accommodated at this time.</p>

                        <div class="booking-card">
                            <div class="warning-badge">⚠ DECLINED</div>
                            <div class="detail-row">
                                <span class="label">Service Type:</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">Location:</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">Provider:</span>
                                <span class="value">%s</span>
                            </div>
                        </div>

                        <p><strong>Don't worry!</strong> There are many other service providers available on FarmEazy.</p>

                        <p><strong>Next Steps:</strong></p>
                        <ul>
                            <li>Browse other available service listings</li>
                            <li>Try booking with different providers</li>
                            <li>Adjust your requirements if needed</li>
                            <li>Contact our support if you need assistance</li>
                        </ul>

                        <a href="%s/irrigation-services" class="action-button">Browse Services</a>

                        <p style="margin-top: 30px; color: #6b7280; font-size: 14px;">
                            The provider may have declined due to scheduling conflicts or service area limitations. Please try booking with another provider.
                        </p>

                        <div class="footer">
                            <p>Thank you for using FarmEazy!</p>
                            <p><a href="%s" style="color: #22c55e;">www.farm-eazy.com</a></p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, userName, serviceType, location, providerName, appBaseUrl, appBaseUrl);

        return sendEmail(userEmail, subject, html);
    }
    // --- END STUBS ---

    private static final Logger logger = LoggerFactory.getLogger(HttpEmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final int CONNECT_TIMEOUT_MS = 5000;  // 5 seconds
    private static final int READ_TIMEOUT_MS = 10000;    // 10 seconds

    @Value("${resend.api.key:}")
    private String resendApiKey;

    // Use verified domain email - must match domain verified in Resend dashboard
    @Value("${resend.from.email:FarmEazy <no-reply@farm-eazy.com>}")
    private String fromEmail;

    @Value("${farmeazy.mail.enabled:true}")
    private boolean emailEnabled;

    @Value("${farmeazy.app.base-url:https://www.farm-eazy.com}")
    private String appBaseUrl;

    private final RestTemplate restTemplate;

    public HttpEmailService() {
        // Configure RestTemplate with timeouts to prevent long waits
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Send email using Resend HTTP API
     * @throws EmailDeliveryException if email sending fails
     */
    public boolean sendEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Would have sent to: {}", to);
            throw new EmailDeliveryException(
                "Email service is currently disabled. Please contact support.",
                "EMAIL_SERVICE_DISABLED",
                to
            );
        }

        String apiKey = System.getenv("RESEND_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = resendApiKey;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("RESEND_API_KEY not configured. Email not sent to: {}", to);
            throw new EmailDeliveryException(
                "Email service is not properly configured. Please contact support.",
                "EMAIL_SERVICE_NOT_CONFIGURED",
                to
            );
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("from", fromEmail);
            emailData.put("to", List.of(to));
            emailData.put("subject", subject);
            emailData.put("html", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                RESEND_API_URL,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Email sent successfully to: {}", to);
                return true;
            } else {
                logger.error("Failed to send email. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
                throw new EmailDeliveryException(
                    "Failed to send email. Please try again later.",
                    "EMAIL_DELIVERY_FAILED",
                    to
                );
            }

        } catch (HttpClientErrorException e) {
            // Handle 4xx errors (domain not verified, invalid API key, etc.)
            logger.error("Email API client error for {}: {} - {}", to, e.getStatusCode(), e.getResponseBodyAsString());
            String errorMessage = parseResendErrorMessage(e.getResponseBodyAsString());
            throw new EmailDeliveryException(
                errorMessage != null ? errorMessage : "Email service configuration error. Please contact support.",
                "EMAIL_API_ERROR",
                to
            );
        } catch (HttpServerErrorException e) {
            // Handle 5xx errors (service unavailable)
            logger.error("Email API server error for {}: {} - {}", to, e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmailDeliveryException(
                "Email service is temporarily unavailable. Please try again later.",
                "EMAIL_SERVICE_UNAVAILABLE",
                to
            );
        } catch (EmailDeliveryException e) {
            // Re-throw our own exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Error sending email to {}: {}", to, e.getMessage());
            throw new EmailDeliveryException(
                "Unable to send email at this time. Please try again later.",
                to,
                e
            );
        }
    }

    /**
     * Parse error message from Resend API response
     */
    private String parseResendErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        try {
            // Simple JSON parsing for "message" field
            if (responseBody.contains("\"message\"")) {
                int start = responseBody.indexOf("\"message\":");
                if (start != -1) {
                    start = responseBody.indexOf("\"", start + 10) + 1;
                    int end = responseBody.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        String message = responseBody.substring(start, end);
                        // Make the message user-friendly
                        if (message.contains("domain is not verified")) {
                            return "Email service configuration issue. Our team has been notified. Please try again later or contact support.";
                        }
                        return message;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not parse Resend error message: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Send welcome email to new user (async - does not block caller)
     */
    @Async
    public CompletableFuture<Boolean> sendWelcomeEmailAsync(String userEmail, String userName) {
        try {
            boolean result = sendWelcomeEmail(userEmail, userName);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async welcome email failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send welcome email to new user
     */
    public boolean sendWelcomeEmail(String userEmail, String userName) {
        String subject = "Welcome to FarmEazy! 🌾";
        String html = buildWelcomeEmailHtml(userName);
        return sendEmail(userEmail, subject, html);
    }

    /**
     * Send password reset email (this one should be sync to ensure delivery before response)
     */
    public boolean sendPasswordResetEmail(String userEmail, String shortCode) {
        String subject = "Reset Your FarmEazy Password";
        // Use the redirect URL with short code - the frontend will resolve it to the full JWT token
        String resetLink = appBaseUrl + "/r/" + shortCode;
        String html = buildPasswordResetEmailHtml(resetLink);
        return sendEmail(userEmail, subject, html);
    }

    /**
     * Build welcome email HTML
     */
    private String buildWelcomeEmailHtml(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #22c55e, #16a34a); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 28px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; background: #22c55e; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🌾 Welcome to FarmEazy!</h1>
                    </div>
                    <div class="content">
                        <h2>Hello, %s! 👋</h2>
                        <p>Thank you for joining FarmEazy - your smart farm management solution!</p>
                        <p>With FarmEazy, you can:</p>
                        <ul>
                            <li>🌱 Track your crops and their growth stages</li>
                            <li>💧 Manage irrigation schedules</li>
                            <li>🏡 Organize multiple farms</li>
                            <li>📊 Monitor farm activities</li>
                        </ul>
                        <p>Get started by logging into your dashboard:</p>
                        <a href="%s/login" class="button">Go to Dashboard</a>
                        <p>If you have any questions, feel free to reach out!</p>
                        <p>Happy Farming! 🚜</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, appBaseUrl);
    }

    /**
     * Build password reset email HTML
     */
    private String buildPasswordResetEmailHtml(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #3b82f6, #1d4ed8); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; background: #3b82f6; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .warning { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 10px 15px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>We received a request to reset your FarmEazy password.</p>
                        <p>Click the button below to set a new password:</p>
                        <a href="%s" class="button">Reset Password</a>
                        <div class="warning">
                            <strong>⚠️ Important:</strong> This link expires in 1 hour.
                        </div>
                        <p>If you didn't request this, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetLink);
    }

    /**
     * Send general notification email (async - does not block caller)
     */
    @Async
    public CompletableFuture<Boolean> sendNotificationAsync(String userEmail, String userName, String subject, String message) {
        try {
            boolean result = sendNotification(userEmail, userName, subject, message);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async notification email failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send general notification email
     */
    public boolean sendNotification(String userEmail, String userName, String subject, String message) {
        String html = buildNotificationEmailHtml(userName, subject, message);
        return sendEmail(userEmail, subject, html);
    }

    /**
     * Send OTP email
     */
    public boolean sendOtpEmail(String userEmail, String userName, String otpCode, String purpose) {
        String subject = "Your FarmEazy OTP Code - " + purpose;
        String html = buildOtpEmailHtml(userName, otpCode, purpose);
        return sendEmail(userEmail, subject, html);
    }

    /**
     * Send product listing confirmation email (async - does not block caller)
     */
    @Async
    public CompletableFuture<Boolean> sendProductListingConfirmationAsync(String userEmail, String userName, String productName,
                                                   String category, Double price, 
                                                   Integer quantity, String unit) {
        try {
            boolean result = sendProductListingConfirmation(userEmail, userName, productName, category, price, quantity, unit);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async product listing email failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send product listing confirmation email
     */
    public boolean sendProductListingConfirmation(String userEmail, String userName, String productName,
                                                   String category, Double price, 
                                                   Integer quantity, String unit) {
        String subject = "Product Listed Successfully - " + productName;
        String html = buildProductListingEmailHtml(userName, productName, category, price, quantity, unit);
        return sendEmail(userEmail, subject, html);
    }

    /**
     * Send order confirmation email (async - does not block caller)
     */
    @Async
    public CompletableFuture<Boolean> sendOrderConfirmationEmailAsync(String userEmail, String userName, Long orderId, String subtotal, String coinsDiscount, String taxAmount, String finalAmount) {
        try {
            boolean result = sendOrderConfirmationEmail(userEmail, userName, orderId, subtotal, coinsDiscount, taxAmount, finalAmount);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async order confirmation email failed for {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send order confirmation email with detailed breakdown
     */
    public boolean sendOrderConfirmationEmail(String userEmail, String userName, Long orderId, String subtotal, String coinsDiscount, String taxAmount, String finalAmount) {
        String subject = "Order Confirmed #FZ" + orderId + " - FarmEazy";
        String html = buildOrderConfirmationEmailHtml(userName, orderId, subtotal, coinsDiscount, taxAmount, finalAmount);
        return sendEmail(userEmail, subject, html);
    }

    /**
     * Build notification email HTML
     */
    private String buildNotificationEmailHtml(String userName, String subject, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #22c55e, #16a34a); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .message-box { background: white; padding: 20px; border-radius: 8px; border-left: 4px solid #22c55e; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🌾 %s</h1>
                    </div>
                    <div class="content">
                        <p>Hello, %s! 👋</p>
                        <div class="message-box">
                            <p>%s</p>
                        </div>
                        <p style="margin-top: 20px;">Visit your dashboard to view more details.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(subject, userName, message);
    }

    /**
     * Build OTP email HTML
     */
    private String buildOtpEmailHtml(String userName, String otpCode, String purpose) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #8b5cf6, #6d28d9); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; text-align: center; }
                    .otp-box { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .otp-code { font-size: 36px; font-weight: bold; color: #8b5cf6; letter-spacing: 8px; }
                    .warning { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 10px 15px; margin: 15px 0; text-align: left; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Your OTP Code</h1>
                    </div>
                    <div class="content">
                        <p>Hello, %s! 👋</p>
                        <p>Your one-time password for <strong>%s</strong>:</p>
                        <div class="otp-box">
                            <div class="otp-code">%s</div>
                        </div>
                        <div class="warning">
                            <strong>⚠️ Important:</strong> This code expires in 10 minutes. Do not share it with anyone.
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, purpose, otpCode);
    }

    /**
     * Build product listing confirmation email HTML
     */
    private String buildProductListingEmailHtml(String userName, String productName, String category,
                                                 Double price, Integer quantity, String unit) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 650px; margin: 0 auto; background: #ffffff; }
                    .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; }
                    .header p { margin: 10px 0 0 0; opacity: 0.9; font-size: 14px; }
                    .content { padding: 40px 30px; background: #fafafa; }
                    .greeting { font-size: 18px; color: #111827; margin-bottom: 20px; }
                    .message { color: #4b5563; margin-bottom: 30px; line-height: 1.8; }
                    .details-card { background: white; border-radius: 12px; padding: 25px; margin: 25px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                    .card-title { font-size: 20px; font-weight: 700; color: #f59e0b; margin: 0 0 20px 0; border-bottom: 2px solid #f59e0b; padding-bottom: 10px; }
                    table { width: 100%%; border-collapse: collapse; }
                    td { padding: 12px 0; border-bottom: 1px solid #e5e7eb; }
                    td:first-child { color: #6b7280; font-weight: 500; width: 40%%; }
                    td:last-child { color: #111827; font-weight: 600; text-align: right; }
                    tr:last-child td { border-bottom: none; }
                    .price-highlight { font-size: 24px; color: #f59e0b; font-weight: 700; }
                    .button { display: inline-block; background: linear-gradient(135deg, #f59e0b, #d97706); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; margin: 25px 0; font-weight: 600; font-size: 16px; box-shadow: 0 4px 6px rgba(245, 158, 11, 0.25); }
                    .info-box { background: #dbeafe; border-left: 4px solid #3b82f6; padding: 16px; border-radius: 6px; margin: 25px 0; }
                    .info-box p { margin: 0; color: #1e40af; font-size: 14px; }
                    .success-badge { display: inline-block; background: #dcfce7; color: #16a34a; padding: 8px 16px; border-radius: 20px; font-weight: bold; margin: 20px 0; }
                    .footer { background: #111827; color: #9ca3af; padding: 30px; text-align: center; font-size: 14px; }
                    .footer a { color: #f59e0b; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📦 Product Listed Successfully!</h1>
                        <p>Your product is now live on the marketplace</p>
                    </div>
                    <div class="content">
                        <div class="greeting">Hello %s! 👋</div>
                        <div class="message">
                            Great news! Your product has been successfully listed on FarmEazy Marketplace. Buyers can now discover and purchase your product.
                        </div>

                        <center>
                            <div class="success-badge">✓ LIVE ON MARKETPLACE</div>
                        </center>

                        <div class="details-card">
                            <h2 class="card-title">📦 Product Details</h2>
                            <table>
                                <tr>
                                    <td>Product Name</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Category</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Price per Unit</td>
                                    <td class="price-highlight">₹%s</td>
                                </tr>
                                <tr>
                                    <td>Available Quantity</td>
                                    <td>%d %s</td>
                                </tr>
                                <tr>
                                    <td>Total Value</td>
                                    <td class="price-highlight">₹%.2f</td>
                                </tr>
                            </table>
                        </div>

                        <div class="info-box">
                            <p><strong>💡 Next Steps:</strong></p>
                            <p style="margin-top: 8px;">• Monitor buyer inquiries in your dashboard</p>
                            <p>• Keep your stock quantities updated</p>
                            <p>• Respond promptly to orders to build trust</p>
                        </div>

                        <center>
                            <a href="%s/selling" class="button">View My Products</a>
                        </center>

                        <div class="message" style="margin-top: 30px; font-size: 14px; color: #6b7280;">
                            Your product is now searchable by buyers looking for "%s" products. Good luck with your sales!
                        </div>
                    </div>
                    <div class="footer">
                        <p><strong>FarmEazy</strong> - Smart Farm Management</p>
                        <p style="margin-top: 10px;">
                            <a href="%s">Visit Website</a> |
                            <a href="%s/buying">Browse Products</a> |
                            <a href="%s/support">Support</a>
                        </p>
                        <p style="margin-top: 15px; font-size: 12px;">© 2026 FarmEazy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, productName, category, price, quantity, unit, (price * quantity), appBaseUrl, category, appBaseUrl, appBaseUrl, appBaseUrl);
    }

    /**
     * Build order confirmation email HTML with detailed pricing breakdown
     */
    private String buildOrderConfirmationEmailHtml(String userName, Long orderId, String subtotal, String coinsDiscount, String taxAmount, String finalAmount) {
        // Calculate coin discount display
        String discountDisplay = (coinsDiscount != null && !coinsDiscount.equals("0"))
            ? String.format("<div class=\"price-row discount\"><span>Coin Discount:</span><span>- ₹%s</span></div>", coinsDiscount)
            : "";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #22c55e, #16a34a); padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .order-box { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .check-icon { font-size: 48px; text-align: center; color: #22c55e; }
                    .order-id { font-size: 14px; color: #6b7280; text-align: center; }
                    .order-number { font-size: 24px; font-weight: bold; color: #22c55e; text-align: center; }
                    .price-breakdown { background: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .price-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #e5e7eb; }
                    .price-row.discount { color: #059669; font-weight: 600; }
                    .price-row.tax { color: #6b7280; }
                    .price-row.total { border-top: 2px solid #22c55e; border-bottom: none; font-size: 20px; font-weight: bold; color: #22c55e; padding-top: 15px; margin-top: 10px; }
                    .info-box { background: #dbeafe; border-left: 4px solid #3b82f6; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .footer { text-align: center; margin-top: 20px; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Order Confirmed!</h1>
                    </div>
                    <div class="content">
                        <p>Hello, %s! 👋</p>
                        <p>Thank you for your order! Your purchase has been confirmed.</p>

                        <div class="order-box">
                            <div class="check-icon">✓</div>
                            <div class="order-id">Order ID</div>
                            <div class="order-number">#FZ%d</div>
                        </div>

                        <div class="price-breakdown">
                            <h3 style="margin-top: 0; color: #374151;">Price Breakdown</h3>
                            <div class="price-row"><span>Subtotal:</span><span>₹%s</span></div>
                            %s
                            <div class="price-row tax"><span>Tax & Charges:</span><span>₹%s</span></div>
                            <div class="price-row total"><span>Final Amount:</span><span>₹%s</span></div>
                        </div>

                        <div class="info-box">
                            <strong>📦 Delivery Information</strong>
                            <p style="margin: 5px 0 0 0;">Expected delivery: 3-5 business days</p>
                            <p style="margin: 5px 0 0 0;">Status: Processing</p>
                        </div>

                        <p>You can track your order status in your dashboard.</p>
                        <p style="margin-top: 20px;">Thank you for shopping with FarmEazy! 🌾</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 FarmEazy. Smart Farm Management.</p>
                        <p style="margin-top: 5px;">Questions? Reply to <a href="mailto:support@farm-eazy.com" style="color: #22c55e;">support@farm-eazy.com</a></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, orderId, subtotal, discountDisplay, taxAmount, finalAmount);
    }
}

