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
        // 1. Konkan Railway & Dudhsagar
        articles.add(new BlogArticleDTO(
            "1",
            "konkan-railway-scenic-odyssey",
            "The Konkan Railway Odyssey: Waterfalls, Tunnels, and Coastal Horizons",
            "A triumph of modern civil engineering winding through 2,000 bridges, 91 tunnels, and the roaring Dudhsagar Falls in the Western Ghats.",
            "Scenic Routes",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2c/Dudhsagar_Falls_Triplet.jpg/960px-Dudhsagar_Falls_Triplet.jpg",
            "Antigravity Rail Bureau",
            "Railway Historian & Travel Journalist",
            6,
            "Oct 24, 2026",
            true,
            List.of("Konkan", "Western Ghats", "Dudhsagar", "Goa", "Scenic Rail"),
            List.of(
                new QuickFact("Length", "741 Kilometers", "route"),
                new QuickFact("Key Feature", "Dudhsagar Falls & Panval Viaduct", "bridge"),
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
                    "Carving a railway line through the treacherous basalt cliffs, unforgiving rivers, and soft clay soil of the Konkan coast was long deemed impossible by British colonial engineers. It was not until late 1990 under visionary engineer Dr. E. Sreedharan that the Konkan Railway Corporation Limited (KRCL) accomplished one of independent India's most ambitious infrastructure projects.",
                    null, null, null
                ),
                new ArticleSection(
                    "image",
                    "The Iconic Dudhsagar Falls Rail Crossing",
                    null,
                    "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1a/Dudhsagar_Falls_triplet%2C_2009.JPG/960px-Dudhsagar_Falls_triplet%2C_2009.JPG",
                    "Indian Railways triplet diesel locomotives hauling passenger coaches across the bridge directly before the thundering Dudhsagar Falls.",
                    "Indian Railways train passing Dudhsagar Falls"
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
                    "trivia",
                    "Did You Know?",
                    "Konkan Railway pioneered the Roll-on/Roll-off (RORO) service in India, allowing loaded freight trucks to be driven directly onto open flat wagons, drastically saving fuel and reducing highway carbon emissions.",
                    null, null, null
                )
            )
        ));

        // 2. Vande Bharat Express
        articles.add(new BlogArticleDTO(
            "2",
            "vande-bharat-evolution-future-sleeper",
            "Vande Bharat 2.0 & Beyond: Inside India's High-Speed Rail Revolution",
            "From indigenous Kavach collision protection to aerodynamic nose-cones: how Train 18 changed the face of Indian mobility.",
            "Train Tech",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/43/Vande_Bharat_Express_train.jpg/960px-Vande_Bharat_Express_train.jpg",
            "AeroRail Engineering Desk",
            "Senior Rolling-Stock Specialist",
            7,
            "Oct 26, 2026",
            true,
            List.of("Vande Bharat", "Train 18", "Kavach", "Make In India", "High Speed"),
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
                    "Vande Bharat Express at an Indian Station",
                    null,
                    "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/59/Howrah_Puri_Vande_Bharat_Express_standing_at_Bhadrak.jpg/960px-Howrah_Puri_Vande_Bharat_Express_standing_at_Bhadrak.jpg",
                    "The aerodynamic Howrah-Puri Vande Bharat Express standing along the platform at Bhadrak railway station.",
                    "Howrah Puri Vande Bharat Express at station platform"
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

        // 3. Pamban Sea Bridge (NEW)
        articles.add(new BlogArticleDTO(
            "3",
            "pamban-sea-bridge-rameswaram",
            "The Engineering Marvel Across The Sea: The Legendary Pamban Rail Bridge",
            "India's historic 2-kilometer cantilever sea bridge braving the roaring waves of the Palk Strait to holy Rameswaram Island.",
            "Scenic Routes",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b5/Train_on_the_Pamban_rail_bridge.jpg/960px-Train_on_the_Pamban_rail_bridge.jpg",
            "Maritime & Coastal Rail Desk",
            "Infrastructure & Coastal Rail Specialist",
            7,
            "Oct 28, 2026",
            true,
            List.of("Pamban Bridge", "Rameswaram", "Sea Bridge", "Tamil Nadu", "Historic Rail"),
            List.of(
                new QuickFact("Length", "2.06 Kilometers (143 Spans)", "route"),
                new QuickFact("Opened", "February 24, 1914", "calendar"),
                new QuickFact("Lift Type", "Scherzer Rolling Lift Cantilever", "bridge"),
                new QuickFact("Location", "Palk Strait (Mandapam to Pamban)", "compass")
            ),
            List.of(
                new RelatedTrain("16851", "Rameswaram Boat Mail Express", "Chennai Egmore → Rameswaram"),
                new RelatedTrain("22661", "Sethu Superfast Express", "Chennai Egmore → Rameswaram")
            ),
            List.of(
                new ArticleSection(
                    "paragraph",
                    null,
                    "Opened in 1914, the Pamban Rail Bridge was India's first sea bridge and remained its longest until the Bandra-Worli Sea Link opened in 2010. Connecting the Indian mainland town of Mandapam with the sacred island of Rameswaram across the turbulent Palk Strait, this 2.06-kilometer structure is one of the most breathtaking railway journeys on Earth.",
                    null, null, null
                ),
                new ArticleSection(
                    "image",
                    "Train Gliding Over Ocean Waves",
                    null,
                    "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/Pamban_Bridge_Train_Passing.jpg/960px-Pamban_Bridge_Train_Passing.jpg",
                    "A passenger train slowly traversing the open ocean waters over the Pamban cantilever spans.",
                    "Indian Railways train crossing Pamban Sea Bridge"
                ),
                new ArticleSection(
                    "heading",
                    "The Genius of the Scherzer Rolling Lift Span",
                    "To allow ships and cargo vessels to pass through the strait, German engineer William Scherzer designed a central double-leaf bascule section. Weighing hundreds of tons, the two massive iron arms could be manually cranked open to create a wide navigational channel for boats.",
                    null, null, null
                ),
                new ArticleSection(
                    "tip",
                    "Window Watching Tip",
                    "As the train crawls onto the Pamban Bridge at a cautious 10-15 km/h, keep your camera ready. The sea stretches endlessly on both sides, with fishing trawlers bobbing below and sea salt mist entering the coach windows.",
                    null, null, null
                ),
                new ArticleSection(
                    "trivia",
                    "The New Vertical-Lift Pamban Bridge",
                    "Indian Railways has constructed a modern state-of-the-art replacement bridge featuring India's first automated vertical lift span, designed to clear ships 22 meters above sea level while supporting electrified high-speed express trains.",
                    null, null, null
                )
            )
        ));

        // 4. CSMT Mumbai (NEW)
        articles.add(new BlogArticleDTO(
            "4",
            "csmt-mumbai-unesco-gothic-wonder",
            "Crown Jewel of Victorian Gothic: The Living Legend of CSMT Mumbai",
            "Stepping inside the bustling heart of Central Railway — a UNESCO World Heritage palace handling 3 million commuters daily.",
            "Heritage & History",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6d/Chhatrapati_Shivaji_Maharaj_Terminus_2.jpg/960px-Chhatrapati_Shivaji_Maharaj_Terminus_2.jpg",
            "Urban Heritage Directorate",
            "Architectural Historian",
            6,
            "Oct 29, 2026",
            false,
            List.of("CSMT", "Mumbai", "UNESCO", "Victorian Gothic", "Heritage"),
            List.of(
                new QuickFact("Built", "1878 to 1887 (10 Years)", "calendar"),
                new QuickFact("Architect", "Frederick William Stevens", "award"),
                new QuickFact("UNESCO Status", "Inscribed in 2004", "shield"),
                new QuickFact("Platforms", "18 Platforms (Suburban & Mail)", "layers")
            ),
            List.of(
                new RelatedTrain("12138", "Punjab Mail", "Firozpur Cantt → CSMT Mumbai"),
                new RelatedTrain("12322", "Kolkata Mail", "CSMT Mumbai → Howrah Jn"),
                new RelatedTrain("22221", "CSMT NZM Rajdhani", "CSMT Mumbai → Hazrat Nizamuddin")
            ),
            List.of(
                new ArticleSection(
                    "paragraph",
                    null,
                    "Originally named Victoria Terminus to honor Queen Victoria's Golden Jubilee in 1887, Chhatrapati Shivaji Maharaj Terminus (CSMT) is an extraordinary fusion of High Victorian Gothic Revival architecture and traditional Indian palace design. Its ribbed stone vaults, stained glass windows, and grand central dome make it one of the most photographed railway stations in the world.",
                    null, null, null
                ),
                new ArticleSection(
                    "image",
                    "CSMT Illuminated in Tricolor Glory",
                    null,
                    "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/38/Chhatrapati_Shivaji_Maharaj_Terminus_at_night%2C_Mumbai%2C_Maharashtra%2C_India_%282018%29.jpg/960px-Chhatrapati_Shivaji_Maharaj_Terminus_at_night%2C_Mumbai%2C_Maharashtra%2C_India_%282018%29.jpg",
                    "The majestic exterior of CSMT lit in the Indian tricolor during evening hours in South Mumbai.",
                    "CSMT illuminated at night in Mumbai"
                ),
                new ArticleSection(
                    "heading",
                    "Gargoyles, Peacocks, and Stone Lions",
                    "Look closely at the stone facades and arches: alongside traditional European gargoyles, master Indian craftsmen carved indigenous peacocks, monkeys, cobra serpents, and elephants into the stone capitals, blending Indian flora and fauna into Victorian buttresses.",
                    null, null, null
                ),
                new ArticleSection(
                    "trivia",
                    "Platform 1 to 18: Suburban vs Long-Distance",
                    "CSMT operates as a dual-hub: Platforms 1 to 7 are dedicated exclusively to Mumbai's lifeline suburban local trains, while Platforms 8 to 18 handle iconic long-distance express trains connecting Mumbai to every corner of India.",
                    null, null, null
                )
            )
        ));

        // 5. Kalka-Shimla Mountain Railway
        articles.add(new BlogArticleDTO(
            "5",
            "kalka-shimla-unesco-toy-train",
            "Whispers of the Himalayas: The Historic Kalka-Shimla Mountain Railway",
            "Traversing 102 tunnels and 869 bridges into Himachal's pine-scented hills on a century-old UNESCO World Heritage track.",
            "Heritage & History",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b1/Shimla%2C_India%2C_Shimla_train.jpg/960px-Shimla%2C_India%2C_Shimla_train.jpg",
            "Heritage & Culture Wing",
            "Mountain Railway Historian",
            8,
            "Oct 28, 2026",
            true,
            List.of("Kalka-Shimla", "Toy Train", "UNESCO", "Himalayas", "Himachal"),
            List.of(
                new QuickFact("Track Gauge", "Narrow Gauge (2 ft 6 in / 762 mm)", "layers"),
                new QuickFact("Elevation Gain", "656m to 2,076m (1,420m rise)", "trending-up"),
                new QuickFact("Tunnels", "102 Tunnels (Barog: 1,143m)", "maximize"),
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
                    "Historic Shimla Railway Station",
                    null,
                    "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0f/Shimla_Railway_Station%2C_Himachal_Pradesh.jpg/960px-Shimla_Railway_Station%2C_Himachal_Pradesh.jpg",
                    "The heritage Shimla railway station nestled under the deodar-covered slopes of Himachal Pradesh.",
                    "Shimla Railway Station Himachal Pradesh"
                ),
                new ArticleSection(
                    "heading",
                    "The Ghost and Glory of Barog Tunnel (Tunnel No. 33)",
                    "The longest tunnel on the route is the Barog Tunnel (1,143 meters). Legend has it that British engineer Colonel Barog began digging from both ends of the mountain, but miscalculations led to the two headings failing to meet. Distraught, he committed suicide near the tunnel. The project was subsequently completed by Chief Engineer H.S. Harington with the guidance of a local shepherd named Bhalku, who accurately tapped the rock walls with his wooden staff.",
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

        // 6. Nilgiri Mountain Railway (NEW)
        articles.add(new BlogArticleDTO(
            "6",
            "nilgiri-mountain-railway-rack-pinion",
            "The Steam Queens of the Nilgiris: Blue Mountain Rack-and-Pinion Railway",
            "Ascending the steepest railway gradient in Asia through eucalyptus hills and tea gardens to Udhagamandalam (Ooty).",
            "Scenic Routes",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d6/Nilgiri_Mountain_Railway_on_Bridge%2C_May_2010.JPG/960px-Nilgiri_Mountain_Railway_on_Bridge%2C_May_2010.JPG",
            "Nilgiri Heritage Trust",
            "Heritage Locomotion Archivist",
            7,
            "Oct 29, 2026",
            false,
            List.of("Nilgiri", "Ooty", "Toy Train", "Steam Train", "UNESCO"),
            List.of(
                new QuickFact("System", "Abt Rack and Pinion Cogwheel", "settings"),
                new QuickFact("Steepest Grade", "1 in 12.5 (8.88% incline)", "trending-up"),
                new QuickFact("Elevation", "Mettupalayam (326m) → Ooty (2,203m)", "route"),
                new QuickFact("UNESCO Status", "Inscribed in 2005", "award")
            ),
            List.of(
                new RelatedTrain("06136", "MTP UAM Special", "Mettupalayam → Udhagamandalam (Ooty)"),
                new RelatedTrain("06137", "UAM MTP Special", "Udhagamandalam (Ooty) → Mettupalayam")
            ),
            List.of(
                new ArticleSection(
                    "paragraph",
                    null,
                    "The Nilgiri Mountain Railway (NMR) in Tamil Nadu is the only rack railway in India and operates on the steepest gradient in Asia. To climb from Mettupalayam in the plains up to Ooty in the clouds (an altitude leap of over 1,800 meters), the train engages a center toothed rack rail with locomotive cogwheels, pushing coaches safely uphill.",
                    null, null, null
                ),
                new ArticleSection(
                    "image",
                    "Classic Blue Rake on Heritage Tracks",
                    null,
                    "https://upload.wikimedia.org/wikipedia/commons/c/c9/Nilgiri_Mountain_Train.jpg",
                    "The historic blue-and-cream coaches of Nilgiri Mountain Railway chugging past tea plantations near Coonoor.",
                    "Nilgiri Mountain Railway train in Coonoor"
                ),
                new ArticleSection(
                    "heading",
                    "The Locomotive Pushes From Behind!",
                    "Unlike normal trains where the locomotive pulls from the front, on the uphill journey from Mettupalayam to Coonoor, the steam locomotive pushes the train from behind! This is a fail-safe safety measure: if a coach ever uncouples, the heavy engine prevents it from rolling backwards down the mountain.",
                    null, null, null
                ),
                new ArticleSection(
                    "tip",
                    "Booking Advice for Ooty Toy Train",
                    "With only one or two toy train departures each day, First Class (FC) and Second Class (2S) tickets sell out months in advance on IRCTC. Set an alarm for 120 days before your travel date to secure your seats.",
                    null, null, null
                )
            )
        ));

        // 7. Tatkal Rules & Hacks
        articles.add(new BlogArticleDTO(
            "7",
            "mastering-irctc-tatkal-booking-rules",
            "Mastering the Tatkal Rush: Timings, Quotas & Pro Booking Strategies",
            "Decode IRCTC opening bells, master passenger pre-filling, and understand cancellation charges to secure confirmed berths every time.",
            "Travel Tips",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7b/Train_on_Platform_3_of_Patna_Junction.jpg/960px-Train_on_Platform_3_of_Patna_Junction.jpg",
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
                    "image",
                    "Bustling Indian Railway Platform",
                    null,
                    "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/43/A_Passenger_train_standing_on_PF_3_of_Patna_Junction.jpg/960px-A_Passenger_train_standing_on_PF_3_of_Patna_Junction.jpg",
                    "A passenger train at platform 3 of Patna Junction as travelers board with their confirmed reservation tickets.",
                    "Passenger train standing on platform at Indian railway station"
                ),
                new ArticleSection(
                    "heading",
                    "The Two Golden Windows: 10:00 AM vs 11:00 AM",
                    "Tatkal quota opens exactly one day in advance of the train's departure date from its originating station:\n• AC Classes (2A, 3A, 3E, CC, EC): Opens at 10:00 AM IST.\n• Non-AC Classes (Sleeper SL, Second Sitting 2S): Opens at 11:00 AM IST.",
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

        // 8. Trackside Gastronomy
        articles.add(new BlogArticleDTO(
            "8",
            "iconic-station-food-indian-railways",
            "Flavors on Track: The Culinary Pilgrimage of Indian Railways",
            "From Ratlam's fiery Sev to Kharagpur's spiced Chai and Guntakal's Mirchi Bajji — a foodie's trackside guide.",
            "Food & Culture",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e1/Indian_samosa.jpg/960px-Indian_samosa.jpg",
            "Rail Culinary Society",
            "Food & Travel Chronicler",
            6,
            "Oct 29, 2026",
            false,
            List.of("Food On Track", "Station Food", "Samosa", "Chai", "Pantry Car"),
            List.of(
                new QuickFact("Famous Chai", "Kharagpur & Varanasi Kulhad Chai", "coffee"),
                new QuickFact("Crispy Snack", "Ratlam Sev & Platform Samosa", "utensils"),
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
                    "Freshly Fried Platform Snacks",
                    null,
                    "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4d/Indian_Street_Food.JPG/960px-Indian_Street_Food.JPG",
                    "Authentic Indian street food and hot snacks prepared fresh along station concourses and food stalls.",
                    "Indian street food hot snacks"
                ),
                new ArticleSection(
                    "heading",
                    "The Legendary Station Halt Delicacies",
                    "• Ratlam Junction (Madhya Pradesh): World-famous clove-infused Ratlam Sev and steaming yellow Poha topped with fresh pomegranate seeds.\n• Kharagpur & Bardhaman (West Bengal): Sitaram bhog and golden Sitabhog sweets along with clay-pot malai chai.\n• Guntakal & Vijayawada (Andhra Pradesh): Spicy Mirchi Bajjis accompanied by sweet cut mangoes and filter coffee.\n• Maddur (Karnataka): Maddur Vada, a crispy onion-semolina fritter born at Maddur station in 1917.",
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
