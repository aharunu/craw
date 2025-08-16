
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

public class InsiderJobCrawlerMultithread {
    private static final int MAX_POSTINGS = 15; 
    private static final int THREAD_POOL_SIZE = 5; 
    private static final AtomicInteger processedCount = new AtomicInteger(0);
    
    public static void main(String[] args) {
        String baseUrl = "https://jobs.lever.co/useinsider";
        
    System.out.println("MULTITHREADED Crawler başlatılıyor...");

        try {
            // Ana sayfayı çek 
            Document doc = Jsoup.connect(baseUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();
            
            // Tüm ilan linklerini bul
            Elements jobLinks = doc.select("a.posting-title");
            System.out.println("Toplam ilan sayısı: " + jobLinks.size());
            System.out.println("İşlenecek ilan sayısı: " + Math.min(jobLinks.size(), MAX_POSTINGS));
            System.out.println("Thread sayısı: " + THREAD_POOL_SIZE);

            if (!jobLinks.isEmpty()) {
                // Create thread pool
                ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
                CountDownLatch latch = new CountDownLatch(Math.min(jobLinks.size(), MAX_POSTINGS));
                
                int jobsToProcess = Math.min(jobLinks.size(), MAX_POSTINGS);
                for (int i = 0; i < jobsToProcess; i++) {
                    Element job = jobLinks.get(i);
                    executor.submit(() -> {
                        try {
                            processJob(job);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                
                executor.shutdown();
                try {
                    latch.await(); // Wait for all jobs to complete
                    if (!executor.awaitTermination(300, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            } else {
                System.out.println("Hiç ilan bulunamadı.");
            }
        
            System.out.println("\n" + "=".repeat(50));
            System.out.println("MULTITHREADED Crawler tamamlandı!");
            System.out.println("Toplam işlenen ilan: " + processedCount.get());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void processJob(Element job) {
        try {
            int currentJobNumber = processedCount.incrementAndGet();
            
            Element card = job.closest("div.posting");
            
            String jobTitle = job.select("h2, h5").text();
            String jobLocation = card.select(".posting-categories .location").text();
            String commitment = card.select(".posting-categories .sort-by-commitment").text();
            String type = card.select(".posting-categories .workplaceTypes").text();
            String team = "";
            
            String jobUrl = job.attr("href");
            
            if (jobUrl != null && !jobUrl.isEmpty()) {
                try {
                    Document detail = Jsoup.connect(jobUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                            .timeout(10000)
                            .get();
                    team = detail.select(".posting-categories .sort-by-team ").text();
                } catch (Exception ex) {

                }
            }

            // Detay sayfasını çek
            Document jobDetail = Jsoup.connect(jobUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            Elements sections = jobDetail.select("div.section.page-centered");
            
            synchronized (System.out) {
                System.out.println("\n=== İlan " + currentJobNumber + "/" + MAX_POSTINGS + " (Thread: " + Thread.currentThread().getName() + ") ===");
                System.out.println("Title: " + jobTitle);
                System.out.println("Location: " + jobLocation);
                System.out.println("URL: " + jobUrl);
                System.out.println("Commitment: " + commitment);
                System.out.println("Team: " + team);
                System.out.println("Type: " + type);

                for (Element section : sections) {
                    String sectionTitle = section.select("h3").text();
                    if (sectionTitle.isEmpty() && section.hasAttr("data-qa") && 
                        section.attr("data-qa").equals("job-description")) {
                        System.out.println("\nFirst:");
                        String jobDescription = section.text();
                        System.out.println(jobDescription);
                    }
                    // Check if this is the closing description
                    else if (sectionTitle.isEmpty() && section.hasAttr("data-qa") && 
                        section.attr("data-qa").equals("closing-description")) {
                        System.out.println("\nClosing Description:");
                        String closingText = section.text();
                        System.out.println(closingText);
                    }
                    // Regular sections with titles
                    else if (!sectionTitle.isEmpty()) {
                        System.out.println("\n" + sectionTitle + ":");
                        Elements listItems = section.select("ul.posting-requirements.plain-list li");
                        if (!listItems.isEmpty()) {
                            for (Element item : listItems) {
                                String bulletPoint = item.text();
                                if (!bulletPoint.isEmpty()) {
                                    System.out.println("• " + bulletPoint);
                                }
                            }
                        } else {
                            // If no bullet points, print as regular content
                            String sectionContent = section.select("div.content").text();
                            System.out.println(sectionContent);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            synchronized (System.err) {
                System.err.println("Error processing job in thread " + Thread.currentThread().getName() + ": " + e.getMessage());
            }
        }
    }
}
