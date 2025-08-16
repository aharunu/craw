import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class InsiderJobCrawlerOne {
    public static void main(String[] args) {
        String baseUrl = "https://jobs.lever.co/useinsider";

        try {
            // Ana sayfayı çek (User-Agent ile)
            Document doc = Jsoup.connect(baseUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();
            
            
            // Tüm ilan linklerini bul
            Elements jobLinks = doc.select("a.posting-title");
            System.out.println("Toplam ilan sayısı: " + jobLinks.size());

            for(Element job : jobLinks) {
                if (!jobLinks.isEmpty()) {

                Element card = job.closest("div.posting");           // ilana ait kart
        

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


                System.out.println("\nTitle: " + jobTitle);
                System.out.println("Location: " + jobLocation);
                System.out.println("URL: " + jobUrl);
                System.out.println("Commitment: " + commitment);
                System.out.println("Team: " + team);
                System.out.println("type: " + type);

                // 1 saniye bekle
                Thread.sleep(1000);

                // Detay sayfasını çek
                Document jobDetail = Jsoup.connect(jobUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                        .timeout(10000)
                        .get();

                
                Elements sections = jobDetail.select("div.section.page-centered");
                for (Element section : sections) {
                    String sectionTitle = section.select("h3").text();
                    if (sectionTitle.isEmpty() && section.hasAttr("data-qa") && 
                    section.attr("data-qa").equals("job-description")){
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
            } else {
                System.out.println("Hiç ilan bulunamadı.");
            }
        }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
