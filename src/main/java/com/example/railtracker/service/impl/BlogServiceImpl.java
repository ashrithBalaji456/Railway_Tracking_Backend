package com.example.railtracker.service.impl;

import com.example.railtracker.dto.BlogArticleDTO;
import com.example.railtracker.dto.BlogArticleDTO.ArticleSection;
import com.example.railtracker.dto.BlogArticleDTO.QuickFact;
import com.example.railtracker.dto.BlogArticleDTO.RelatedTrain;
import com.example.railtracker.service.BlogService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    private final List<BlogArticleDTO> articles = new ArrayList<>();

    public BlogServiceImpl() {
        initArticles();
    }

    private void initArticles() {
        // 1. Konkan Railway
        articles.add(new BlogArticleDTO(
            "1",
            "konkan-railway-scenic-odyssey",
            "The Konkan Railway Odyssey: Waterfalls, Tunnels, and Coastal Horizons",
            "A triumph of modern civil engineering winding through 2,000 bridges, 91 tunnels, and the lush mist of the Western Ghats.",
            "Scenic Routes",
            "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80",
            "Antigravity Rail Bureau",
            "Railway Historian & Travel Journalist",
            6,
            "Oct 24, 2026",
            true,
            List.of("Konkan", "Western Ghats", "Scenic Rail", "Monsoon Travel", "Dudhsagar"),
            List.of(
                new QuickFact("Length", "741 Kilometers", "route"),
                new QuickFact("Key Feature", "Panval Nadi Viaduct (64m)", "bridge"),
                new QuickFact("Best Season", "July to October (Monsoon)", "calendar"),
                new QuickFact("Tunnels", "91 Tunnels (Longest: 6.5 km)", "tunnel")
            ),
            List.of(
                new RelatedTrain("10103", "Mandovi Express", "CSMT Mumbai → Madgaon Jn (Goa)"),
                new RelatedTrain("12051", "Madgaon Jan Shatabdi", "CSMT Mumbai → Madgaon Jn"),
                new RelatedTrain("22229", "Goa Vande Bharat Express", "CSMT Mumbai → Madgaon Jn")
            ),
            List.of(
                new ArticleSection(
                    "paragraph",
                    null,
                    "Carving a railway line through the treacherous basalt cliffs, unforgiving rivers, and soft clay soil of the Konkan coast was long deemed impossible by British colonial engineers. It wasn't until late 1990 under visionary engineer Dr. E. Sreedharan that the Konkan Railway Corporation Limited (KRCL) accomplished one of independent India's most ambitious infrastructure projects.",
                    null, null, null
                ),
                new ArticleSection(
                    "image",
                    "Through The Heart of Western Ghats",
                    null,
                    "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?auto=format&fit=crop&w=1000&q=80",
                    "A passenger train navigating the lush green valleys and serpentine curves of the Konkan terrain.",
                    "Train traversing lush green mountains on Konkan Railway"
                ),
                new ArticleSection(
                    "heading",
                    "Engineering Marvels: Panval Nadi & Karbude Tunnel",
                    "The route features the legendary Panval Nadi Viaduct near Ratnagiri, which stands at an awe-inspiring height of 64 meters above the riverbed — taller than the Qutub Minar. Furthermore, the 6.5-kilometer Karbude Tunnel was the longest railway tunnel in India for over a decade, requiring specialized rock-blasting shields and ventilation shafts.",
                    null, null, null
                ),
                new ArticleSection(
                    "tip",
                    "Traveler's Seating Hack",
                    "When travelling south from Mumbai to Goa, book window seats on the RIGHT side (seats facing West) for breathtaking glimpses of the Arabian Sea and setting sun. In monsoon, the left side offers cascading mountain streams.",
                    null, null, null
                ),
                new ArticleSection(
                    "paragraph",
                    null,
                    "During the monsoons (July to September), the Konkan route transforms into an emerald paradise. Waterfalls cascade right next to the tracks, mist clings to the viaducts, and the aroma of wet earth fills the coaches. Indian Railways enforces a special 'Monsoon Timetable' with reduced speeds for safety, making it a leisurely, unforgettable retreat.",
                    null, null, null
                ),
                new ArticleSection(
                    "image",
                    "Monsoon Cascades and Mist",
                    null,
                    "https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1000&q=80",
                    "Morning mist enshrouding the Konkan trackbed during peak monsoon season.",
                    "Misty morning along the Konkan route"
                ),
                new ArticleSection(
                    "trivia",
                    "Did You Know?",
                    "Konkan Railway pioneered the Roll-on/Roll-off (RORO) service in India, allowing loaded freight trucks to be driven directly onto open flat wagons, drastically saving fuel and reducing highway carbon emissions.",
                    null, null, null
                )
            )
        ));

        // 2. Vande Bharat Tech
        articles.add(new BlogArticleDTO(
            "2",
            "vande-bharat-evolution-future-sleeper",
            "Vande Bharat 2.0 & Beyond: Inside India's High-Speed Rail Revolution",
            "From indigenous Kavach collision protection to overnight sleeper rakes: how Train 18 changed the face of Indian mobility.",
            "Train Tech",
            "https://images.unsplash.com/photo-1474487548417-781cb71495f3?auto=format&fit=crop&w=1200&q=80",
            "AeroRail Engineering Desk",
            "Senior Rolling-Stock Specialist",
            7,
            "Oct 26, 2026",
            true,
            List.of("Vande Bharat", "Train 18", "Kavach", "Sleeper", "Innovation"),
            List.of(
                new QuickFact("Top Test Speed", "180 km/h (In-Service: 130-160 km/h)", "gauge"),
                new QuickFact("Acceleration", "0 to 100 km/h in 52 seconds", "zap"),
                new QuickFact("Safety System", "Kavach (SIL-4 Certified ATP)", "shield"),
                new QuickFact("Braking", "Regenerative Electro-Pneumatic", "battery")
            ),
            List.of(
                new RelatedTrain("20607", "MAS MYS Vande Bharat", "MGR Chennai Central → Mysuru Jn"),
                new RelatedTrain("22436", "Vande Bharat Express", "New Delhi → Varanasi Jn"),
                new RelatedTrain("20641", "Bengaluru - Coimbatore VB", "KSR Bengaluru → Coimbatore Jn")
            ),
            List.of(
                new ArticleSection(
                    "paragraph",
                    null,
                    "In October 2018, when Integral Coach Factory (ICF) in Chennai rolled out prototype 'Train 18', few expected it would ignite a renaissance in domestic rail manufacturing. Designed and built completely in India under the 'Make in India' initiative in just 18 months at nearly half the cost of imported trainsets, Vande Bharat proved that Indian engineering could rival global benchmarks.",
                    null, null, null
                ),
                new ArticleSection(
                    "heading",
                    "Distributed Power: The Secret Behind Rapid Acceleration",
                    "Unlike traditional trains with a heavy locomotive hauling passive coaches, Vande Bharat uses a Distributed Traction Power system. Alternating motorized coaches along the rake deliver uniform propulsion, cutting 0-100 km/h acceleration time to just 52 seconds — faster than a standard bullet train.",
                    null, null, null
                ),
                new ArticleSection(
                    "image",
                    "Aerodynamic Aesthetics & Ergonomics",
                    null,
                    "https://images.unsplash.com/photo-1541427468627-a89a96e5ca1d?auto=format&fit=crop&w=1000&q=80",
                    "The modern cockpit and streamlined nose-cone engineered to minimize aerodynamic drag at 160 km/h.",
                    "Vande Bharat aerodynamic cab"
                ),
                new ArticleSection(
                    "trivia",
                    "Kavach Safety Armor",
                    "Vande Bharat trainsets are equipped with Kavach, India's indigenous Automatic Train Protection (ATP) system. If the loco pilot fails to brake at a red signal or if another train enters the same block section, Kavach automatically commands the brakes to prevent collision.",
                    null, null, null
                ),
                new ArticleSection(
                    "heading",
                    "The Upcoming Vande Bharat Sleeper: Redefining Overnight Journeys",
                    "Designed for long-haul routes spanning 800 to 1,500 km, the upcoming Vande Bharat Sleeper trains will feature whisper-quiet sealed vestibules, superior suspension damping, USB-C and inductive charging at every berth, and ergonomic modular toilets, bringing Rajdhani luxury into a new era of speed.",
                    null, null, null
                )
            )
        ));

        // 3. Tatkal Rules & Hacks
        articles.add(new BlogArticleDTO(
            "3",
            "mastering-irctc-tatkal-booking-rules",
            "Mastering the Tatkal Rush: Timings, Quotas & Pro Booking Strategies",
            "Decode IRCTC opening bells, master passenger pre-filling, and understand cancellation charges to secure confirmed berths every time.",
            "Travel Tips",
            "https://images.unsplash.com/photo-1532105956626-9569c03602f6?auto=format&fit=crop&w=1200&q=80",
            "Ticket Intelligence Unit",
            "Ticketing & Fares Analyst",
            5,
            "Oct 27, 2026",
            false,
            List.of("Tatkal", "IRCTC", "Ticket Booking", "Travel Tips", "Fare Rules"),
            List.of(
                new QuickFact("AC Tatkal Window", "10:00 AM (1 Day Prior)", "clock"),
                new QuickFact("Non-AC Tatkal Window", "11:00 AM (1 Day Prior)", "clock"),
                new QuickFact("Charge Surcharge", "10% to 30% of Base Fare", "credit-card"),
                new QuickFact("Confirmed Refund", "₹0 (Zero Refund on Confirmed)", "alert-triangle")
            ),
            List.of(
                new RelatedTrain("12723", "Telangana Superfast", "Hyderabad Deccan → New Delhi"),
                new RelatedTrain("12791", "SC DNR SF Express", "Secunderabad Jn → Danapur")
            ),
            List.of(
                new ArticleSection(
                    "paragraph",
                    null,
                    "Every day at 10:00 AM sharp, thousands of travelers across India log into IRCTC for the ultimate test of digital dexterity: booking a Tatkal ticket. Understanding the nuances of Indian Railways Tatkal quota can be the difference between a confirmed berth and a stranded travel plan.",
                    null, null, null
                ),
                new ArticleSection(
                    "heading",
                    "The Two Golden Windows: 10:00 AM vs 11:00 AM",
                    "Tatkal quota opens exactly one day in advance of the train's departure date from its originating station (ARP):\\n• AC Classes (2A, 3A, 3E, CC, EC): Opens at 10:00 AM IST.\\n• Non-AC Classes (Sleeper SL, Second Sitting 2S): Opens at 11:00 AM IST.",
                    null, null, null
                ),
                new ArticleSection(
                    "tip",
                    "The Master List Strategy",
                    "Never enter passenger names and ID details manually at 10:00 AM! Pre-save all passengers in your IRCTC 'Master Passenger List' before 9:50 AM. During checkout, you can auto-fill all names with a single click.",
                    null, null, null
                ),
                new ArticleSection(
                    "paragraph",
                    null,
                    "Payment method selection is crucial. Net-banking or complex OTP cards often time out during peak server load. UPI AutoPay, QR-code scan, or IRCTC eWallet frequently process within seconds without waiting for third-party bank gateways.",
                    null, null, null
                ),
                new ArticleSection(
                    "trivia",
                    "Tatkal Cancellation Rule",
                    "Important to remember: Indian Railways offers NO refund for cancellation of confirmed Tatkal tickets. However, if your Tatkal ticket is waitlisted (WL) after final chart preparation, it gets automatically cancelled and refunded.",
                    null, null, null
                )
            )
        ));

        // 4. Kalka-Shimla Toy Train
        articles.add(new BlogArticleDTO(
            "4",
            "kalka-shimla-unesco-toy-train",
            "Whispers of the Himalayas: The Historic Kalka-Shimla Mountain Railway",
            "Traversing 102 tunnels and 869 bridges into Himachal's pine-scented hills on a century-old UNESCO World Heritage track.",
            "Heritage & History",
            "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80",
            "Heritage & Culture Wing",
            "Mountain Railway Historian",
            8,
            "Oct 28, 2026",
            true,
            List.of("Kalka-Shimla", "Toy Train", "UNESCO", "Himalayas", "Heritage"),
            List.of(
                new QuickFact("Track Gauge", "Narrow Gauge (2 ft 6 in / 762 mm)", "layers"),
                new QuickFact("Elevation Gain", "656m to 2,076m (1,420m rise)", "trending-up"),
                new QuickFact("Tunnels", "102 Tunnels (Barog Tunnel: 1,143m)", "maximize"),
                new QuickFact("UNESCO Status", "Inscribed in July 2008", "award")
            ),
            List.of(
                new RelatedTrain("52457", "Kalka Shimla Passenger", "Kalka (KLK) → Shimla (SML)"),
                new RelatedTrain("52453", "Kalka Shimla Express", "Kalka (KLK) → Shimla (SML)"),
                new RelatedTrain("52459", "Him Darshan Express", "Kalka (KLK) → Shimla (SML)")
            ),
            List.of(
                new ArticleSection(
                    "paragraph",
                    null,
                    "Opened in November 1903 during the British Raj to connect the summer capital of India with the plains, the 96-kilometer Kalka-Shimla Railway is an enduring testament to human grit and railway romance. As the little red diesel locomotive chugs uphill, pine valleys and snow-dusted peaks unfold like poetry.",
                    null, null, null
                ),
                new ArticleSection(
                    "image",
                    "Arch Bridges of Dharampur",
                    null,
                    "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1000&q=80",
                    "Multi-tier stone arch viaduct resembling Roman aqueducts winding around steep alpine slopes.",
                    "Stone arch bridge in Himalayas"
                ),
                new ArticleSection(
                    "heading",
                    "The Ghost and Glory of Barog Tunnel (Tunnel No. 33)",
                    "The longest tunnel on the route is the Barog Tunnel (1,143 meters). Legend has it that British engineer Colonel Barog began digging from both ends of the mountain, but miscalculations led to the two headings failing to meet. Distraught, he committed suicide near the tunnel. The project was subsequently completed by Chief Engineer H.S. Harington with the supernatural guidance of a local shepherd named Bhalku, who accurately tapped the rock walls with his wooden staff.",
                    null, null, null
                ),
                new ArticleSection(
                    "tip",
                    "Experience Him Darshan Vistadome",
                    "For panoramic 360-degree views of the cedar forests, book the Him Darshan Express (Train No. 52459). The coaches have glass ceilings, plush swivel chairs, and window blinds for bright sunny stretches.",
                    null, null, null
                )
            )
        ));

        // 5. Trackside Gastronomy
        articles.add(new BlogArticleDTO(
            "5",
            "iconic-station-food-indian-railways",
            "Flavors on Track: The Culinary Pilgrimage of Indian Railways",
            "From Ratlam's fiery Sev to Kharagpur's spiced Chai and Guntakal's Mirchi Bajji — a foodie's trackside guide.",
            "Food & Culture",
            "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=1200&q=80",
            "Rail Culinary Society",
            "Food & Travel Chronicler",
            6,
            "Oct 29, 2026",
            false,
            List.of("Food On Track", "Station Food", "Indian Cuisine", "Chai", "Pantry Car"),
            List.of(
                new QuickFact("Famous Chai", "Kharagpur & Varanasi Kulhad Chai", "coffee"),
                new QuickFact("Crispy Snack", "Ratlam Sev & Poha", "utensils"),
                new QuickFact("South Specialty", "Idli Vada at Vijayawada & Jolarpettai", "heart"),
                new QuickFact("Sweet Treat", "Maddur Vada & Bardhaman Mihidana", "gift")
            ),
            List.of(
                new RelatedTrain("12138", "Punjab Mail", "Firozpur Cantt → CSMT Mumbai"),
                new RelatedTrain("12840", "Howrah Mail", "MGR Chennai Central → Howrah Jn")
            ),
            List.of(
                new ArticleSection(
                    "paragraph",
                    null,
                    "To travel on Indian Railways is to undertake a multi-course culinary journey through the subcontinent. Long before modern app delivery, passengers judged the quality of their voyage by the vendor cries echoing across station platforms: 'Garam Chai!', 'Ratlam ki Mashhoor Sev!', and 'Garma Garam Samosa!'",
                    null, null, null
                ),
                new ArticleSection(
                    "image",
                    "Clay Cup Chai on the Platform",
                    null,
                    "https://images.unsplash.com/photo-1546833999-b9f581a1996d?auto=format&fit=crop&w=1000&q=80",
                    "Steaming hot spiced milk tea served in an earthen clay kulhad on a bustling railway platform.",
                    "Indian spiced chai in kulhad"
                ),
                new ArticleSection(
                    "heading",
                    "The Legendary Station Halt Delicacies",
                    "• Ratlam Junction (Madhya Pradesh): World-famous clove-infused Ratlam Sev and steaming yellow Poha topped with fresh pomegranate seeds.\\n• Kharagpur & Bardhaman (West Bengal): Sitaram bhog and golden Sitabhog sweets along with clay-pot malai chai.\\n• Guntakal & Vijayawada (Andhra Pradesh): Spicy Mirchi Bajjis accompanied by sweet cut mangoes and filter coffee.\\n• Maddur (Karnataka): Maddur Vada, a crispy onion-semolina fritter born at Maddur station in 1917.",
                    null, null, null
                ),
                new ArticleSection(
                    "tip",
                    "E-Catering Delivery to Berth",
                    "Did you know you can order fresh meals from top city restaurants directly to your seat? Use the IRCTC e-Catering portal or WhatsApp service with your PNR number, and delivery partners deliver hot food right at your intermediate station halt.",
                    null, null, null
                )
            )
        ));
    }

    @Override
    public List<BlogArticleDTO> getAllArticles(String category, String query) {
        return articles.stream()
            .filter(a -> category == null || category.equalsIgnoreCase("All") || a.category().equalsIgnoreCase(category))
            .filter(a -> {
                if (query == null || query.isBlank()) return true;
                String q = query.toLowerCase().trim();
                return a.title().toLowerCase().contains(q)
                    || a.subtitle().toLowerCase().contains(q)
                    || a.tags().stream().anyMatch(t -> t.toLowerCase().contains(q))
                    || a.category().toLowerCase().contains(q);
            })
            .collect(Collectors.toList());
    }

    @Override
    public Optional<BlogArticleDTO> getArticleBySlug(String slug) {
        if (slug == null) return Optional.empty();
        return articles.stream()
            .filter(a -> a.slug().equalsIgnoreCase(slug) || a.id().equalsIgnoreCase(slug))
            .findFirst();
    }

    @Override
    public List<BlogArticleDTO> getFeaturedArticles() {
        return articles.stream()
            .filter(BlogArticleDTO::featured)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getCategories() {
        return List.of("All", "Scenic Routes", "Train Tech", "Heritage & History", "Travel Tips", "Food & Culture");
    }
}
