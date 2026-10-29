package com.example.railtracker.service.impl;

import com.example.railtracker.dto.BlogArticleDTO;
import com.example.railtracker.dto.BlogTipDTO;
import com.example.railtracker.service.BlogService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    private final List<BlogArticleDTO> articles = new ArrayList<>();
    private final Map<String, List<BlogTipDTO>> tipsMap = new ConcurrentHashMap<>();

    public BlogServiceImpl() {
        initArticles();
        initTips();
    }

    private void initArticles() {
        // Story 1: konkan-railway-scenic-odyssey
        articles.add(new BlogArticleDTO(
            "1",
            "konkan-railway-scenic-odyssey",
            "The Konkan Railway Odyssey: Waterfalls, Viaducts & Coastal Cliffs",
            "Traversing 760 kilometers through 91 tunnels and 2,000 bridges across the formidable Western Ghats.",
            "Scenic Routes",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b3/Dudhsagar_Falls_train.jpg/960px-Dudhsagar_Falls_train.jpg",
            "Dr. E. Sreedharan & RailTrack Editorial",
            "Chief Infrastructure Editor",
            8,
            "2026-10-24",
            true,
            List.of("Konkan Railway", "Western Ghats", "Dudhsagar", "Engineering Marvel", "Monsoon Express"),
            List.of(new BlogArticleDTO.QuickFact("Route Length", "760 km", "map"), new BlogArticleDTO.QuickFact("Total Tunnels", "91 (84 km underground)", "tunnel"), new BlogArticleDTO.QuickFact("Longest Tunnel", "Karbude (6.5 km)", "compass"), new BlogArticleDTO.QuickFact("Tallest Viaduct", "Panval River (64 m tall)", "bridge")),
            List.of(new BlogArticleDTO.RelatedTrain("10103", "Mandovi Express", "CSMT to MAO"), new BlogArticleDTO.RelatedTrain("12051", "Jan Shatabdi Express", "CSMT to MAO"), new BlogArticleDTO.RelatedTrain("22229", "Goa Vande Bharat", "CSMT to MAO")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. The Impossible Engineering Feat", "1. The Impossible Engineering Feat", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "For over a century, British and post-independence engineers deemed connecting Mumbai directly to Mangaluru along the narrow, river-gashed Sahyadri coastline impossible. It required boring through treacherous soft lithomargic clay that liquefied under torrential monsoon downpours. In 1990, the Konkan Railway Corporation Limited (KRCL) was born under the legendary technocrat Dr. E. Sreedharan, delivering one of the 20th century's boldest infrastructure triumphs in just seven years.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/33/Konkan_Railway_Western_Ghats.jpg/960px-Konkan_Railway_Western_Ghats.jpg", "A diesel triplet locomotive hauling an express train along the emerald cliffs and viaducts of the Konkan Railway.", "A diesel triplet locomotive hauling an express train along the emerald cliffs and viaducts of the Konkan Railway."),
                new BlogArticleDTO.ArticleSection("heading", "2. Roaring Dudhsagar & The Braganza Incline", "2. Roaring Dudhsagar & The Braganza Incline", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Where the railway crosses the Goa-Karnataka border, the Mandovi River plunges down a catastrophic 310-meter tiered precipice known as Dudhsagar ('Sea of Milk'). As your train rumbles over the masonry arched bridge right in front of the waterfall spray, clouds of chilled mist envelop the open windows in a roar of white thunder.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a4/Panval_Nadi_Viaduct_Ratnagiri.jpg/960px-Panval_Nadi_Viaduct_Ratnagiri.jpg", "The Panval Nadi Viaduct near Ratnagiri, soaring 64 meters above the valley floor as Asia's tallest slip-form concrete bridge.", "The Panval Nadi Viaduct near Ratnagiri, soaring 64 meters above the valley floor as Asia's tallest slip-form concrete bridge."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Book tickets on the right-hand window side when traveling South (Mumbai towards Goa) to catch breathtaking Arabian Sea horizon vistas and lush coconut groves.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. Towering Viaducts & Karwar Sea Estuaries", "3. Towering Viaducts & Karwar Sea Estuaries", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Near Ratnagiri, the Panval River Viaduct rises 64 meters into the sky on hollow concrete piers. Further south at Karwar, the train glides across the Kali River estuary where turquoise river channels meet the azure ocean waves against backdrop fishing trawlers.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e0/Karwar_Kali_River_Railway_Bridge.jpg/960px-Karwar_Kali_River_Railway_Bridge.jpg", "Train crossing the long-span Kali River railway bridge entering Karwar, Karnataka.", "Train crossing the long-span Kali River railway bridge entering Karwar, Karnataka."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? The Konkan Railway pioneered RORO (Roll-On Roll-Off) service in India, where loaded freight trucks drive directly onto flat railway wagons to save diesel, tyres, and coastal highway congestion.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "Konkan Railway is not merely a track of steel; it is India's defiance against impossible terrain and an eternal ode to engineering genius.", null, null, null)
            )
        ));

        // Story 2: vande-bharat-evolution-future-sleeper
        articles.add(new BlogArticleDTO(
            "2",
            "vande-bharat-evolution-future-sleeper",
            "Vande Bharat 2.0 & Beyond: Revolutionizing Indian Rail Mobility",
            "From semi-high speed day flyers with indigenous Kavach protection to aerodynamic 160 km/h overnight sleepers.",
            "Train Tech",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b5/Vande_Bharat_Express_at_platform.jpg/960px-Vande_Bharat_Express_at_platform.jpg",
            "Sudhanshu Mani & Tech Rail Desk",
            "Principal Rolling Stock Designer",
            7,
            "2026-10-22",
            true,
            List.of("Vande Bharat", "Train 18", "Kavach", "Make In India", "High Speed", "Modernization"),
            List.of(new BlogArticleDTO.QuickFact("Test Speed", "180 km/h (Trial) / 160 km/h (Commercial)", "gauge"), new BlogArticleDTO.QuickFact("0 to 100 km/h", "52 seconds (Faster than bullet train)", "zap"), new BlogArticleDTO.QuickFact("Braking System", "Electro-pneumatic regenerative", "shield"), new BlogArticleDTO.QuickFact("Design & Build", "Integral Coach Factory (ICF), Chennai", "cpu")),
            List.of(new BlogArticleDTO.RelatedTrain("20607", "MGR Chennai - Mysuru Vande Bharat", "MAS to MYS"), new BlogArticleDTO.RelatedTrain("22436", "New Delhi - Varanasi Vande Bharat", "NDLS to BSB"), new BlogArticleDTO.RelatedTrain("20641", "Bengaluru - Coimbatore Vande Bharat", "SBC to CBE")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. The Birth of Train 18 at ICF Chennai", "1. The Birth of Train 18 at ICF Chennai", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Conceived and executed by General Manager Sudhanshu Mani and his team at Integral Coach Factory (ICF) Chennai in a record 18 months at one-third the global import cost, Train 18 revolutionized Indian passenger expectations. Eliminating the bulky locomotive, its distributed traction power spreads driving motors across alternating coaches beneath the carriage floor.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a5/Vande_Bharat_aerodynamic_nose.jpg/960px-Vande_Bharat_aerodynamic_nose.jpg", "Aerodynamic driver nose cone with high-impact crash attenuator and digital pilot console.", "Aerodynamic driver nose cone with high-impact crash attenuator and digital pilot console."),
                new BlogArticleDTO.ArticleSection("heading", "2. World-Class Passenger Ergonomics", "2. World-Class Passenger Ergonomics", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Inside, passenger comforts mirror world-class aviation: fully sealed gangways, automatic sliding plug doors that lock before train movement, touchless bio-vacuum lavatories, diffused LED lighting, and 180-degree rotatable reclining seats in Executive Class that pivot towards the scenery.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c8/Vande_Bharat_Executive_Chair_Car.jpg/960px-Vande_Bharat_Executive_Chair_Car.jpg", "The plush 2+2 Executive Chair Car with motorized reclining seats and panoramic anti-glare tinted windows.", "The plush 2+2 Executive Chair Car with motorized reclining seats and panoramic anti-glare tinted windows."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Executive Chair Car (EC) seats 19 to 32 rotate 180 degrees. If traveling in a group, you can face each other across the generous aisle table.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. Indigenous Kavach Safety Armor & Sleeper Horizons", "3. Indigenous Kavach Safety Armor & Sleeper Horizons", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Every Vande Bharat 2.0 trainset comes pre-fitted with Kavach — India's indigenously developed Automatic Train Protection (ATP) system using RFID tags and radio transceivers. If two trains ever approach the same track section, Kavach automatically applies the brakes without driver intervention. The upcoming 16-coach Vande Bharat Sleeper version will redefine overnight luxury between metropolitan capitals.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1b/Vande_Bharat_crossing_bridge.jpg/960px-Vande_Bharat_crossing_bridge.jpg", "Vande Bharat Express smoothly gliding over a river bridge at 130 km/h cruising speed.", "Vande Bharat Express smoothly gliding over a river bridge at 130 km/h cruising speed."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? Vande Bharat hits 0 to 100 km/h in just 52 seconds — out-accelerating the Japanese Shinkansen bullet train which takes 54 seconds.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "Vande Bharat proved to the world that India can design, engineer, and build world-standard passenger trainsets entirely from our own soil.", null, null, null)
            )
        ));

        // Story 3: pamban-sea-bridge-rameswaram
        articles.add(new BlogArticleDTO(
            "3",
            "pamban-sea-bridge-rameswaram",
            "The Pamban Sea Bridge: Engineering Across Ocean Waves to Rameswaram",
            "India’s historic 2.06 km ocean cantilever bridge and the revolutionary new vertical-lift marvel.",
            "Heritage & History",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7b/Pamban_Bridge_Rameswaram_train.jpg/960px-Pamban_Bridge_Rameswaram_train.jpg",
            "Maritime Engineering Bureau",
            "Marine Rail Historian",
            6,
            "2026-10-18",
            true,
            List.of("Pamban Bridge", "Rameswaram", "Sea Bridge", "Scherzer Lift", "Southern Railway"),
            List.of(new BlogArticleDTO.QuickFact("Total Spans", "143 concrete and steel spans", "anchor"), new BlogArticleDTO.QuickFact("Bridge Length", "2,065 meters (6,774 ft)", "ruler"), new BlogArticleDTO.QuickFact("Opening Year", "1914 (Original Scherzer bridge)", "calendar"), new BlogArticleDTO.QuickFact("Wind Speed Alert", "Auto-signals red if winds > 58 km/h", "wind")),
            List.of(new BlogArticleDTO.RelatedTrain("16851", "Boat Mail Express", "MS to RMM"), new BlogArticleDTO.RelatedTrain("22661", "Sethu Superfast Express", "MS to RMM")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. Gliding Over Palk Strait Waves", "1. Gliding Over Palk Strait Waves", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Rumbling over the turquoise ocean waters of the Palk Strait, the Pamban Bridge connects mainland Mandapam to the holy island of Rameswaram. Completed in 1914, it was India's first sea bridge, braving saltwater corrosion, cyclone fury, and corrosive sea spray for over a century.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5e/Pamban_Scherzer_rolling_lift_span.jpg/960px-Pamban_Scherzer_rolling_lift_span.jpg", "The iconic double-leaf Scherzer rolling lift span opened vertically to allow cargo ships to pass underneath.", "The iconic double-leaf Scherzer rolling lift span opened vertically to allow cargo ships to pass underneath."),
                new BlogArticleDTO.ArticleSection("heading", "2. The Scherzer Rolling Cantilever Wonder", "2. The Scherzer Rolling Cantilever Wonder", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "At the bridge's center sits the legendary Scherzer rolling lift span designed by German engineer William Scherzer. Counterweighted with colossal iron blocks, the two halves were manually cranked open by a team of lockkeepers, lifting the tracks up like arms to allow coastal ships and fishing steamers to cruise through the channel.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cd/Train_over_Pamban_sea_waves.jpg/960px-Train_over_Pamban_sea_waves.jpg", "Passenger coaches gliding inches above ocean spray with panoramic 360-degree sea horizon views.", "Passenger coaches gliding inches above ocean spray with panoramic 360-degree sea horizon views."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Roll down your window on the left side when arriving into Rameswaram. The emerald clarity of the coral sea below the bridge is unlike anywhere else on the subcontinental network.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. The Next-Gen Vertical Lift Marvel", "3. The Next-Gen Vertical Lift Marvel", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "To replace the aging 1914 structure, Indian Railways constructed a brand new 2.08 km sea bridge featuring India's first electro-mechanical vertical lift span. The central 72.5-meter navigational truss span lifts vertically into the sky, accommodating larger ships and future 25 kV AC overhead railway electrification.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2f/New_Pamban_vertical_lift_bridge.jpg/960px-New_Pamban_vertical_lift_bridge.jpg", "The futuristic new Pamban vertical lift bridge rising above the Palk Strait ocean waves.", "The futuristic new Pamban vertical lift bridge rising above the Palk Strait ocean waves."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? In the catastrophic 1964 Rameswaram cyclone, the Pamban rail bridge withstood waves while the nearby town of Dhanushkodi was submerged; the bridge was restored in just 46 days under E. Sreedharan.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "To ride a train over the Pamban sea bridge is to feel the boundary between rail track and open ocean vanish beneath your wheels.", null, null, null)
            )
        ));

        // Story 4: csmt-mumbai-unesco-gothic-wonder
        articles.add(new BlogArticleDTO(
            "4",
            "csmt-mumbai-unesco-gothic-wonder",
            "CSMT Mumbai: The Living Victorian Gothic Legend of Indian Railways",
            "Discover the UNESCO World Heritage terminus where stone gargoyles meet 3 million daily suburban commuters.",
            "Heritage & History",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e8/Chhatrapati_Shivaji_Terminus_Mumbai.jpg/960px-Chhatrapati_Shivaji_Terminus_Mumbai.jpg",
            "Heritage Architecture Trust",
            "Colonial Heritage Curator",
            6,
            "2026-10-15",
            false,
            List.of("CSMT", "Mumbai", "UNESCO Heritage", "Gothic Architecture", "Central Railway"),
            List.of(new BlogArticleDTO.QuickFact("Construction Period", "1878 - 1887 (10 Years)", "clock"), new BlogArticleDTO.QuickFact("Chief Architect", "Frederick William Stevens", "pen"), new BlogArticleDTO.QuickFact("UNESCO Inscription", "2004 World Heritage Site", "award"), new BlogArticleDTO.QuickFact("Daily Footfall", "Over 3,000,000 passengers", "users")),
            List.of(new BlogArticleDTO.RelatedTrain("12138", "Punjab Mail (India's oldest)", "FZR to CSMT"), new BlogArticleDTO.RelatedTrain("12322", "Kolkata Mail", "CSMT to HWH"), new BlogArticleDTO.RelatedTrain("22221", "CSMT Hazrat Nizamuddin Rajdhani", "CSMT to NZM")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. A Cathedral of Modern Steam and Steel", "1. A Cathedral of Modern Steam and Steel", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Originally named Victoria Terminus (VT) and christened in 1887 to celebrate Queen Victoria's Golden Jubilee, Chhatrapati Shivaji Maharaj Terminus (CSMT) is an extravagant synthesis of High Victorian Gothic Revival architecture and traditional Indian palace craftsmanship. Designed by Frederick William Stevens, its construction took ten years using yellow sandstone and Italian marble.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/69/CSMT_illumination_night.jpg/960px-CSMT_illumination_night.jpg", "CSMT illuminated in magnificent tricolor and royal gold facade lighting on a Mumbai evening.", "CSMT illuminated in magnificent tricolor and royal gold facade lighting on a Mumbai evening."),
                new BlogArticleDTO.ArticleSection("heading", "2. Vaulted Domes, Peacocks & Stained Glass", "2. Vaulted Domes, Peacocks & Stained Glass", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "The central octagonal dome is crowned by a colossal 14-foot stone statue of 'Progress' holding a flaming torch and a locomotive wheel. Inside the booking concourse, students from the Sir J.J. School of Art carved indigenous flora and fauna into stone corbels, featuring peacocks, cobras, tigers, and monkeys alongside European gargoyles.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/87/CSMT_stained_glass_vaulted_ceiling.jpg/960px-CSMT_stained_glass_vaulted_ceiling.jpg", "The star-chamber vaulted ceiling with imported 19th-century stained glass and Corinthian marble columns.", "The star-chamber vaulted ceiling with imported 19th-century stained glass and Corinthian marble columns."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Visit the CSMT Rail Heritage Museum on the 1st floor to view original 1880 blueprints, vintage bell clocks, brass steam locomotive models, and historical telegraph equipment.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. The Beating Heart of Mumbai", "3. The Beating Heart of Mumbai", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Unlike European heritage terminals preserved as silent museums, CSMT remains the throbbing arterial nerve center of Mumbai. Across its 18 platforms, over 1,000 suburban EMUs and long-distance superfast expresses arrive and depart daily, channeling millions of commuters through its historic arches.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a2/CSMT_suburban_train_concourse.jpg/960px-CSMT_suburban_train_concourse.jpg", "The bustling suburban concourse handling millions of commuters with seamless precision.", "The bustling suburban concourse handling millions of commuters with seamless precision."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? The original cost of constructing Victoria Terminus in 1887 was ₹16.14 lakh (approx. £260,000) — making it the most expensive building constructed in Bombay in the 19th century.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "CSMT stands as a testament that a masterpiece of classical world architecture can also be the busiest, most democratic railway station on planet Earth.", null, null, null)
            )
        ));

        // Story 5: kalka-shimla-unesco-toy-train
        articles.add(new BlogArticleDTO(
            "5",
            "kalka-shimla-unesco-toy-train",
            "Kalka-Shimla Mountain Railway: 102 Tunnels Through Himalayan Pines",
            "Step into a storybook 2-foot-6-inch gauge mountain railway ascending 1,425 meters into the mist.",
            "Scenic Routes",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a2/Kalka_Shimla_toy_train.jpg/960px-Kalka_Shimla_toy_train.jpg",
            "Himalayan Mountain Rail Club",
            "Mountain Railway Historian",
            7,
            "2026-10-12",
            false,
            List.of("Kalka-Shimla", "Toy Train", "UNESCO", "Himalayas", "Barog Tunnel", "Vistadome"),
            List.of(new BlogArticleDTO.QuickFact("Route Length", "96.6 km (59.7 miles)", "map"), new BlogArticleDTO.QuickFact("Elevation Gain", "656 m at Kalka to 2,076 m at Shimla", "trending-up"), new BlogArticleDTO.QuickFact("Number of Bridges", "864 multi-arch masonry bridges", "bridge"), new BlogArticleDTO.QuickFact("UNESCO Status", "Inscribed in 2008", "award")),
            List.of(new BlogArticleDTO.RelatedTrain("52457", "Kalka - Shimla Passenger", "KLK to SML"), new BlogArticleDTO.RelatedTrain("52453", "Kalka - Shimla NG Express", "KLK to SML"), new BlogArticleDTO.RelatedTrain("52459", "Him Darshan Vistadome Special", "KLK to SML")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. Ascending into the Clouds", "1. Ascending into the Clouds", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Opened in November 1903 under Lord Curzon to connect the Summer Capital of British India, the Kalka-Shimla Railway is a fairytale of narrow-gauge civil engineering. In just 96 kilometers, the track climbs 1,425 meters, negotiating 919 sharp curves and 864 Roman-style multi-tiered masonry viaducts through lush deodar and pine forests.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/24/Barog_railway_station_tunnel_33.jpg/960px-Barog_railway_station_tunnel_33.jpg", "The scenic Barog Railway Station at the exit portal of the historic 1,143-meter Tunnel No. 33.", "The scenic Barog Railway Station at the exit portal of the historic 1,143-meter Tunnel No. 33."),
                new BlogArticleDTO.ArticleSection("heading", "2. The Ghost Legend of Barog Tunnel 33", "2. The Ghost Legend of Barog Tunnel 33", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "The longest tunnel on the line is Tunnel No. 33 at Barog, stretching 1,143 meters straight through mountain rock. The British engineer, Colonel Barog, began boring from both ends simultaneously. When the alignment missed by a few feet, he was fined one rupee and overcome by grief; the tunnel was later completed by Chief Engineer H.S. Harrington with guidance from local saint Bhalku.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/Shimla_railway_station_hills.jpg/960px-Shimla_railway_station_hills.jpg", "Shimla railway station terminus perched amidst pine-covered Himalayan ridges.", "Shimla railway station terminus perched amidst pine-covered Himalayan ridges."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Book the 'Him Darshan Express' (Train 52459) for panoramic glass-roof Vistadome carriages that let you look up directly into towering oak trees and snowy Himalayan peaks.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. Roman Viaducts and Crisp Mountain Air", "3. Roman Viaducts and Crisp Mountain Air", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Traveling at an unhurried 22 km/h, the toy train allows passengers to breathe clean cedar air, hear chirping mountain birds, and purchase steaming hot chai and fresh roasted pakoras during brief station pauses at Dharampur and Solan.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/75/Him_Darshan_Vistadome_toy_train.jpg/960px-Him_Darshan_Vistadome_toy_train.jpg", "The Him Darshan Vistadome trainset rounding a mountain curve with 360-degree glass visibility.", "The Him Darshan Vistadome trainset rounding a mountain curve with 360-degree glass visibility."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? Local mystic Bhalku helped align the Kalka-Shimla railway line by tapping the mountainsides with a solid wooden staff to listen to the rock density.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "There are fast trains that take you across destinations, and then there is the Kalka-Shimla toy train that takes you back in time.", null, null, null)
            )
        ));

        // Story 6: nilgiri-mountain-railway-rack-pinion
        articles.add(new BlogArticleDTO(
            "6",
            "nilgiri-mountain-railway-rack-pinion",
            "Nilgiri Mountain Railway: Blue Mountain Steam on Asia's Steepest Rack Track",
            "Discover the century-old Swiss Abt rack-and-pinion system pushing heritage coaches up into the clouds of Ooty.",
            "Scenic Routes",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c8/Nilgiri_Mountain_Railway_steam_engine.jpg/960px-Nilgiri_Mountain_Railway_steam_engine.jpg",
            "Southern Mountain Rail Guild",
            "Senior Locomotive Engineer",
            7,
            "2026-10-09",
            false,
            List.of("Nilgiri", "Ooty", "Toy Train", "Abt Rack Pinion", "UNESCO", "Steam Loco"),
            List.of(new BlogArticleDTO.QuickFact("Steepest Gradient", "1 in 12.28 (8.15% incline)", "trending-up"), new BlogArticleDTO.QuickFact("Traction System", "Abt Rack & Pinion alternate teeth", "cog"), new BlogArticleDTO.QuickFact("Locomotives", "X-Class oil-fired steam locomotives", "zap"), new BlogArticleDTO.QuickFact("UNESCO Status", "Inscribed in 2005 (Extended DHR)", "award")),
            List.of(new BlogArticleDTO.RelatedTrain("06136", "Mettupalayam - Udhagamandalam Special", "MTP to UAM"), new BlogArticleDTO.RelatedTrain("06137", "Udhagamandalam - Mettupalayam Special", "UAM to MTP")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. Conquering the Blue Mountains", "1. Conquering the Blue Mountains", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Starting at Mettupalayam in the Tamil Nadu plains, the meter-gauge Nilgiri Mountain Railway (NMR) ascends 46 kilometers to Udhagamandalam (Ooty) at an elevation of 2,203 meters. Completed in 1908, it relies on the ingenious Swiss Abt rack-and-pinion system to climb grades of 1 in 12.28, the steepest operational railway gradient in all of Asia.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/30/Abt_rack_rail_cogwheel_NMR.jpg/960px-Abt_rack_rail_cogwheel_NMR.jpg", "The dual-toothed Abt rack rail mounted between the running rails, engaging the locomotive's motorized cogwheels.", "The dual-toothed Abt rack rail mounted between the running rails, engaging the locomotive's motorized cogwheels."),
                new BlogArticleDTO.ArticleSection("heading", "2. The Pusher Steam Engine Phenomenon", "2. The Pusher Steam Engine Phenomenon", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Unlike conventional trains pulled from the front, the Swiss-built X-Class steam locomotive couples to the rear of the train at Mettupalayam and pushes the carriages uphill towards Coonoor. This ensures that in the unlikely event of a mechanical failure, the carriages cannot roll backward down the mountain slope.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4e/NMR_train_passing_tea_gardens_Coonoor.jpg/960px-NMR_train_passing_tea_gardens_Coonoor.jpg", "The blue and cream heritage coaches winding through rolling tea plantations near Coonoor.", "The blue and cream heritage coaches winding through rolling tea plantations near Coonoor."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: The stretch between Kallar and Coonoor features 16 tunnels and 250 bridges over foaming mountain waterfalls. Sit on the valley side for breathtaking gorge photography.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. The Romance of Live Steam and Nilgiri Eucalyptus", "3. The Romance of Live Steam and Nilgiri Eucalyptus", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "As the train negotiates the hairpin curves above Hillgrove station, plumes of fragrant white steam mingle with cool mountain air scented with eucalyptus trees and blossoming wild rhododendrons, creating an unforgettable sensory journey.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/52/Udhagamandalam_Ooty_railway_station.jpg/960px-Udhagamandalam_Ooty_railway_station.jpg", "The historic Udhagamandalam (Ooty) terminus railway station surrounded by Nilgiri hills.", "The historic Udhagamandalam (Ooty) terminus railway station surrounded by Nilgiri hills."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? The famous Bollywood song 'Chaiyya Chaiyya' from Dil Se was filmed on the rooftop of this exact Nilgiri Mountain Railway train as it wound past Coonoor.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "The rhythmic chuff of the steam engine gripping the rack rail is the heartbeat of the Nilgiri hills.", null, null, null)
            )
        ));

        // Story 7: mastering-irctc-tatkal-booking-rules
        articles.add(new BlogArticleDTO(
            "7",
            "mastering-irctc-tatkal-booking-rules",
            "Mastering the Tatkal Rush: Pro Rules, Timers & Master List Shortcuts",
            "Everything you need to know about 10:00 AM AC and 11:00 AM Non-AC quotas, payment gateways, and seat confirmation.",
            "Travel Tips",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/14/Patna_Junction_railway_station_coaches.jpg/960px-Patna_Junction_railway_station_coaches.jpg",
            "IRCTC Ticketing Strategist",
            "Ticketing Operations Analyst",
            5,
            "2026-10-05",
            false,
            List.of("Tatkal", "IRCTC", "Booking Hacks", "Ticket Rules", "Master List", "Travel Hacks"),
            List.of(new BlogArticleDTO.QuickFact("AC Tatkal Window", "Opens 10:00 AM (1 day prior)", "clock"), new BlogArticleDTO.QuickFact("Non-AC Tatkal Window", "Opens 11:00 AM (1 day prior)", "clock"), new BlogArticleDTO.QuickFact("Tatkal Charge", "10% to 30% of base fare (capped)", "credit-card"), new BlogArticleDTO.QuickFact("Refund on CNF Tatkal", "Zero refund on confirmed cancellation", "alert-circle")),
            List.of(new BlogArticleDTO.RelatedTrain("12723", "Telangana Superfast Express", "HYB to NDLS"), new BlogArticleDTO.RelatedTrain("12791", "Secunderabad - Danapur Express", "SC to DNR")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. Understand the T-1 Booking Timeline", "1. Understand the T-1 Booking Timeline", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Tatkal bookings open exactly one day prior to the train's journey origination date from its starting terminal. For AC classes (2A, 3A, 3E, CC, EC), the booking counter opens online at 10:00 AM IST. For Non-AC classes (Sleeper SL, Second Sitting 2S), the window opens at 11:00 AM IST. Notice: If a train originates on Thursday but reaches your boarding stop on Friday, Tatkal opens on Wednesday based on the train origin date.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/87/IRCTC_passenger_reservation_counter.jpg/960px-IRCTC_passenger_reservation_counter.jpg", "An official Indian Railways PRS booking counter during morning rush hours.", "An official Indian Railways PRS booking counter during morning rush hours."),
                new BlogArticleDTO.ArticleSection("heading", "2. The Master List Pre-Filling Secret", "2. The Master List Pre-Filling Secret", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Never type passenger names, ages, and berth preferences during the Tatkal countdown. Log in to your IRCTC account at 09:45 AM, navigate to 'My Profile' > 'Master List', and save all passenger details beforehand. When the clock strikes 10:00 AM, select your passengers from the pre-filled dropdown in just one second.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/23/Electronic_train_reservation_chart_board.jpg/960px-Electronic_train_reservation_chart_board.jpg", "Electronic train reservation display boards showing confirmed coach allocations.", "Electronic train reservation display boards showing confirmed coach allocations."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Always choose UPI AutoPay or IRCTC e-Wallet as your payment mode. Credit/Debit card OTP SMS messages frequently get delayed during morning peak traffic, costing you confirmed seats.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. The Golden Cancellation Rule", "3. The Golden Cancellation Rule", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "No refund is granted on cancellation of confirmed Tatkal tickets. However, if your Tatkal ticket remains on the Waitlist (WL) after final chart preparation, it is automatically cancelled and 100% of the fare is refunded directly to your original payment bank account.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/52/LHB_coach_boarding_platform.jpg/960px-LHB_coach_boarding_platform.jpg", "Passengers boarding modern LHB AC coaches after confirmed chart publication.", "Passengers boarding modern LHB AC coaches after confirmed chart publication."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? First AC (1A) and Executive Anubhuti coaches are strictly excluded from Tatkal booking rules by Indian Railways policy.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "In the Tatkal rush, victory belongs not to the fastest internet connection, but to the passenger with a pre-filled Master List and a fast UPI PIN.", null, null, null)
            )
        ));

        // Story 8: iconic-station-food-indian-railways
        articles.add(new BlogArticleDTO(
            "8",
            "iconic-station-food-indian-railways",
            "Trackside Gastronomy: The Iconic Bites of Indian Railways Platforms",
            "From Ratlam's fiery clove sev and Kharagpur's kulhad chai to Vijayawada's mirchi bajji and hot e-catering.",
            "Food & Culture",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/ba/Samosa_on_plate.jpg/960px-Samosa_on_plate.jpg",
            "Culinary Rail Traveler",
            "Trackside Food Journalist",
            6,
            "2026-10-02",
            false,
            List.of("Station Food", "Ratlam Sev", "Kulhad Chai", "Mirchi Bajji", "IRCTC e-Catering", "Culinary Heritage"),
            List.of(new BlogArticleDTO.QuickFact("Longest Tea Platform", "Kharagpur Jn (Platform 1)", "coffee"), new BlogArticleDTO.QuickFact("Famous Sev Capital", "Ratlam Junction, MP", "award"), new BlogArticleDTO.QuickFact("Fiery Snack", "Vijayawada Mirchi Bajji", "flame"), new BlogArticleDTO.QuickFact("e-Catering Coverage", "Over 350 major stations nationwide", "shopping-bag")),
            List.of(new BlogArticleDTO.RelatedTrain("12951", "Mumbai Tejas Rajdhani", "MMCT to NDLS"), new BlogArticleDTO.RelatedTrain("12839", "Howrah - Chennai Mail", "HWH to MAS")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. Ratlam Sev & Poha: The King of Platform Breakfasts", "1. Ratlam Sev & Poha: The King of Platform Breakfasts", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Whenever long-distance trains pull into Ratlam Junction in Madhya Pradesh, passengers rush to the platform stalls. Spiced with pungent clove ('laung') and black pepper, crunchy golden Ratlam Sev poured over warm turmeric poha with fresh lime is the ultimate platform breakfast.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/14/Poha_with_sev_garnishing.jpg/960px-Poha_with_sev_garnishing.jpg", "Freshly garnished Indori/Ratlam poha topped with coriander, diced onions, and spicy Ratlam Sev.", "Freshly garnished Indori/Ratlam poha topped with coriander, diced onions, and spicy Ratlam Sev."),
                new BlogArticleDTO.ArticleSection("heading", "2. Clay Cup Chai at Kharagpur Junction", "2. Clay Cup Chai at Kharagpur Junction", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "At Kharagpur in West Bengal, home to one of the world's longest railway platforms, scalding hot ginger-cardamom tea brewed in unglazed earthen clay cups ('kulhads') imparts an earthy aroma that no paper or plastic cup could ever replicate.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cd/Indian_tea_in_a_clay_pot.jpg/960px-Indian_tea_in_a_clay_pot.jpg", "Earthen clay kulhads of steaming spiced milk tea brewed fresh at station platform stalls.", "Earthen clay kulhads of steaming spiced milk tea brewed fresh at station platform stalls."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Order fresh meals from top city restaurants directly to your seat using IRCTC e-Catering with your PNR number, and delivery partners deliver hot food right at your intermediate station halt.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. Vijayawada's Mirchi Bajji & Coastal Vada Pav", "3. Vijayawada's Mirchi Bajji & Coastal Vada Pav", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "At Vijayawada Junction along the Krishna River, stalls fry steaming green chilli fritters ('Mirchi Bajji') stuffed with carom seeds and dipped in chickpea batter. Across the western coastline from CSMT to Kalyan, nothing fuels commuters like the classic Batata Vada Pav with spicy garlic powder.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/52/Mirchi_bajji.jpg/960px-Mirchi_bajji.jpg", "Golden deep-fried Andhra Mirchi Bajjis served hot with sliced onions and lemon.", "Golden deep-fried Andhra Mirchi Bajjis served hot with sliced onions and lemon."),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4e/Vada_Pav-Indian_street_food.JPG/960px-Vada_Pav-Indian_street_food.JPG", "The quintessential Mumbai Batata Vada Pav with salted fried green chilli.", "The quintessential Mumbai Batata Vada Pav with salted fried green chilli."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? Traditional clay kulhad cups are 100% natural, biodegradable, and support local village potters across Uttar Pradesh, Bihar, and Bengal.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "The true soul of Indian Railways lives in the whistle of the pressure cooker and the call of 'Garam Chai!' echoing down the platform.", null, null, null)
            )
        ));

        // Story 9: maharajas-express-world-leading-luxury
        articles.add(new BlogArticleDTO(
            "9",
            "maharajas-express-world-leading-luxury",
            "Maharajas' Express: World’s Leading Luxury Train & Royal Palaces on Rails",
            "Step aboard the half-mile-long royal train that redefined luxury with presidential suites, royal dining cars, and private butler service.",
            "Train Tech",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/39/Maharajas%27_Express_Train.jpg/960px-Maharajas%27_Express_Train.jpg",
            "Luxury Travel Connoisseur",
            "Luxury Rail Inspector",
            8,
            "2026-10-28",
            true,
            List.of("Maharajas Express", "Luxury Train", "Palace On Rails", "Presidential Suite", "IRCTC Luxury", "Royal Heritage"),
            List.of(new BlogArticleDTO.QuickFact("Global Honors", "Voted World's Leading Luxury Train 7 years", "award"), new BlogArticleDTO.QuickFact("Train Length", "Nearly half a mile (23 opulent carriages)", "ruler"), new BlogArticleDTO.QuickFact("Top Suite", "Navratna Presidential Suite (entire rail coach)", "crown"), new BlogArticleDTO.QuickFact("Dining Cars", "Rang Mahal & Mayur Mahal (peacock ceiling)", "utensils")),
            List.of(new BlogArticleDTO.RelatedTrain("22221", "Mumbai CSMT Rajdhani", "CSMT to NZM"), new BlogArticleDTO.RelatedTrain("12002", "Bhopal Shatabdi Express", "NDLS to RKMP")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. The Sovereign Palace on Wheels", "1. The Sovereign Palace on Wheels", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Spanning nearly half a mile with 23 burgundy-and-gold carriages, the Maharajas' Express is the world's most awarded luxury train. Operated by IRCTC, it recreates the golden age of Rajput and Mughal emperors, chauffeuring guests through India's most fabled destinations including Agra, Jaipur, Ranthambore, Khajuraho, and Varanasi in supreme royal style.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cc/Luxary_ICF_Train-_Maharajas_Express..jpg/960px-Luxary_ICF_Train-_Maharajas_Express..jpg", "The regal burgundy and gold carriages of the Maharajas' Express gleaming on the platform.", "The regal burgundy and gold carriages of the Maharajas' Express gleaming on the platform."),
                new BlogArticleDTO.ArticleSection("heading", "2. The Navratna Presidential Suite: An Entire Carriage", "2. The Navratna Presidential Suite: An Entire Carriage", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Occupying a full dedicated railway carriage, the Presidential Suite ('Navratna') is the only one of its kind in the world. It features two palatial bedrooms, a private living salon, two full ensuite bathrooms with full-size bathtubs, private butler service, and Wi-Fi, offering complete private luxury at 120 km/h.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/37/Luxary_ICF_Train-_Maharajas_Express._3.jpg/960px-Luxary_ICF_Train-_Maharajas_Express._3.jpg", "Pneumatic suspension coaches offering smooth cruise velocity across India's trunk rail lines.", "Pneumatic suspension coaches offering smooth cruise velocity across India's trunk rail lines."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Every guest receives a dedicated personal butler ('Khidmatgar') dressed in traditional royal attire, available 24/7 at the touch of a call button.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. Gourmet Dining at Mayur Mahal & Rang Mahal", "3. Gourmet Dining at Mayur Mahal & Rang Mahal", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Dining aboard the Maharajas' Express is a feast for the senses. Two fine-dining restaurants — 'Mayur Mahal' with its peacock-feather ceiling motifs and 'Rang Mahal' with royal hand-painted frescoes — serve world-class regional delicacies on gold-rimmed Limoges porcelain with fine crystal glassware.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/be/Luxary_ICF_Train-_Maharajas_Express._5.jpg/960px-Luxary_ICF_Train-_Maharajas_Express._5.jpg", "Hand-carved wood paneling and gold leaf ornamentation along the luxury corridor.", "Hand-carved wood paneling and gold leaf ornamentation along the luxury corridor."),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1d/Maharaja_Express.jpg/960px-Maharaja_Express.jpg", "The observation salon car with plush leather armchairs and panoramic picture windows.", "The observation salon car with plush leather armchairs and panoramic picture windows."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? The Maharajas' Express features advanced pneumatic air-suspension systems specifically calibrated to ensure that not a single drop of wine spills while the train negotiates track crossovers.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "To board the Maharajas' Express is not merely to travel across India; it is to reign over it as royalty.", null, null, null)
            )
        ));

        // Story 10: palace-on-wheels-sovereign-desert-odyssey
        articles.add(new BlogArticleDTO(
            "10",
            "palace-on-wheels-sovereign-desert-odyssey",
            "Palace on Wheels: The Sovereign Desert Odyssey Across Rajasthan",
            "Discover India’s original royal train that turned the private hunting saloons of Maharajas into a desert cruise.",
            "Heritage & History",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b3/Palace_on_wheels.JPG/960px-Palace_on_wheels.JPG",
            "Rajasthan Heritage Board",
            "Desert Circuit Historian",
            7,
            "2026-10-26",
            false,
            List.of("Palace on Wheels", "Rajasthan", "Jaipur", "Udaipur", "Jaisalmer", "Heritage Rails"),
            List.of(new BlogArticleDTO.QuickFact("Inaugural Run", "26 January 1982", "calendar"), new BlogArticleDTO.QuickFact("Coaches Named After", "Former princely states of Rajputana", "crown"), new BlogArticleDTO.QuickFact("Circuit Route", "Delhi - Jaipur - Ranthambore - Udaipur - Jaisalmer", "map"), new BlogArticleDTO.QuickFact("Theme", "Royal Rajputana Palaces", "home")),
            List.of(new BlogArticleDTO.RelatedTrain("12413", "Pooja Superfast Express", "AII to JAT"), new BlogArticleDTO.RelatedTrain("12981", "Chetak Express", "DEE to UDZ")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. The Pioneer of Luxury Rail in India", "1. The Pioneer of Luxury Rail in India", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Launched jointly by the Rajasthan Tourism Development Corporation (RTDC) and Indian Railways in 1982, the Palace on Wheels was India's first luxury train. Originally composed of authentic vintage wooden saloons previously owned by the Nizam of Hyderabad, the Maharaja of Jaipur, and the Viceroy of British India, it introduced travelers to the Thar desert like royalty.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/Palace_on_wheels_2.JPG/960px-Palace_on_wheels_2.JPG", "The gleaming white and gold heritage carriages of the Palace on Wheels.", "The gleaming white and gold heritage carriages of the Palace on Wheels."),
                new BlogArticleDTO.ArticleSection("heading", "2. Saloons Named After Princely Dynasties", "2. Saloons Named After Princely Dynasties", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Each carriage is named after a legendary princely state — Alwar, Bharatpur, Bikaner, Bundi, Dholpur, Dungarpur, Jaisalmer, Jaipur, Jhalawar, Jodhpur, Kishangarh, Kota, Sirohi, and Udaipur. Inside, delicate zardozi velvet embroidery, carved Burma teak woodwork, and antique chandeliers evoke the grandeur of desert fortresses.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/25/Rajasthan_Royal_on_Wheels.jpg/960px-Rajasthan_Royal_on_Wheels.jpg", "Traditional Rajasthani royal motifs and miniature paintings adorning the coach walls.", "Traditional Rajasthani royal motifs and miniature paintings adorning the coach walls."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: In Jaisalmer, the itinerary includes an evening camel ride across the Sam Sand Dunes with traditional Kalbelia dance performances under the starlit desert sky.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. Awaking to a New Fortress Every Dawn", "3. Awaking to a New Fortress Every Dawn", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "The magic of the Palace on Wheels lies in its nocturnal journey rhythm: while guests dine on Laal Maas and sleep in silk-draped four-poster berths, the train covers hundreds of kilometers through the night. Each morning, passengers wake up to a new fortress city — from the pink walls of Jaipur to the golden sandstone towers of Jaisalmer and the tranquil Lake Pichola of Udaipur.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/26/The_Royal_Rajasthan_on_Wheels_train.jpg/960px-The_Royal_Rajasthan_on_Wheels_train.jpg", "Royal Rajasthan on Wheels trainset preparing for departure into the Thar desert.", "Royal Rajasthan on Wheels trainset preparing for departure into the Thar desert."),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/69/Royal_Rajasthan_On_Wheels_%281%29.jpg/960px-Royal_Rajasthan_On_Wheels_%281%29.jpg", "The luxury lounge carriage where guests gather for evening talks and royal aperitifs.", "The luxury lounge carriage where guests gather for evening talks and royal aperitifs."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? The original coaches of the Palace on Wheels used in 1982 were actual 19th-century royal heritage cars that had survived in private railway sheds since the pre-independence era.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "The Palace on Wheels is a rolling poem written across the golden sands of Rajasthan.", null, null, null)
            )
        ));

        // Story 11: darjeeling-himalayan-railway-unesco-toy-train
        articles.add(new BlogArticleDTO(
            "11",
            "darjeeling-himalayan-railway-unesco-toy-train",
            "Darjeeling Himalayan Railway: The UNESCO Steam Toy Train of the Tea Clouds",
            "Discover the 2-foot narrow gauge miracle built in 1881 that spirals up to India’s highest station at Ghum.",
            "Scenic Routes",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5e/Batasia_loop%2C_Darjeeling.jpg/960px-Batasia_loop%2C_Darjeeling.jpg",
            "Darjeeling Rail Heritage Trust",
            "Eastern Himalayan Rail Researcher",
            8,
            "2026-10-25",
            false,
            List.of("Darjeeling", "Toy Train", "UNESCO", "Steam Loco", "Batasia Loop", "Ghum Station", "Himalayas"),
            List.of(new BlogArticleDTO.QuickFact("Gauge", "2 feet (610 mm narrow gauge)", "ruler"), new BlogArticleDTO.QuickFact("Highest Station", "Ghum (2,258 m / 7,407 ft - India's Highest)", "mountain"), new BlogArticleDTO.QuickFact("Inaugural Year", "1881 (World's first mountain railway)", "calendar"), new BlogArticleDTO.QuickFact("UNESCO Inscription", "1999 World Heritage Site", "award")),
            List.of(new BlogArticleDTO.RelatedTrain("52541", "New Jalpaiguri - Darjeeling Passenger", "NJP to DJ"), new BlogArticleDTO.RelatedTrain("52544", "Darjeeling - Kurseong NG Passenger", "DJ to KGN")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. The First Mountain Railway on Earth", "1. The First Mountain Railway on Earth", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Conceived in 1878 by Franklin Prestage, the Darjeeling Himalayan Railway (DHR) was the world's first hill passenger railway. Winding 88 kilometers from New Jalpaiguri in the Bengal plains to Darjeeling at 2,050 meters, it overcame impossible mountain terrain without building expensive tunnels, using loops and 'Z-reverses' that follow the contours of the hill ridges.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/87/Batasia_Loop%2C_Darjeeling_%2801%29.jpg/960px-Batasia_Loop%2C_Darjeeling_%2801%29.jpg", "The famous Batasia Loop where the toy train makes a complete 360-degree circle with views of snow-capped Kanchenjunga.", "The famous Batasia Loop where the toy train makes a complete 360-degree circle with views of snow-capped Kanchenjunga."),
                new BlogArticleDTO.ArticleSection("heading", "2. The Batasia Loop & Ghum at 7,407 Feet", "2. The Batasia Loop & Ghum at 7,407 Feet", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "The most celebrated spot on the line is the Batasia Loop near Ghum. Here, the track loops around a landscaped war memorial garden in a full circle, gently shedding elevation while providing a breathtaking panoramic view of the world's third-highest peak, Mount Kanchenjunga. At Ghum (2,258 m / 7,407 ft), it claims the title of India's highest railway station.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5a/Batasia_Loop%2C_Darjeeling_%2802%29.jpg/960px-Batasia_Loop%2C_Darjeeling_%2802%29.jpg", "Toy train engine whistling through the deodars at the summit of the Batasia loop.", "Toy train engine whistling through the deodars at the summit of the Batasia loop."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Take the daily 'Joy Ride' steam service from Darjeeling to Ghum and back. It halts for 10 minutes at Batasia Loop for photographs and gives you 30 minutes to explore the DHR Rail Museum at Ghum.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. The Immortal B-Class Steam Locomotives", "3. The Immortal B-Class Steam Locomotives", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "What makes Darjeeling truly timeless is its fleet of original B-Class 0-4-0 saddle tank steam locomotives, originally manufactured between 1889 and 1925 by Sharp, Stewart & Co. and the North British Locomotive Company. Fed by hand-shoveled coal, they continue to puff up the slopes through bustling mountain bazaars where shopkeepers pull their awnings back as the train brushes past.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/52/Darjeeling_Himalayan_Railway_Steam_Loco_788.jpg/960px-Darjeeling_Himalayan_Railway_Steam_Loco_788.jpg", "Historic B-Class steam locomotive No. 788 resting on the mountain tracks.", "Historic B-Class steam locomotive No. 788 resting on the mountain tracks."),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/10/Darjeeling_Himalayan_Railway_train%2C_Batasia_loop%2C_India.jpg/960px-Darjeeling_Himalayan_Railway_train%2C_Batasia_loop%2C_India.jpg", "The toy train puffing white steam clouds into the clear crisp morning air of the Himalayas.", "The toy train puffing white steam clouds into the clear crisp morning air of the Himalayas."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? In the early 1900s, two 'sanders' rode on small platforms perched right above the cowcatcher, manually scattering handfuls of dry sand onto the rails when dew made the tracks slippery.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "The Darjeeling toy train is a mechanical work of art that converted an impassable Himalayan barrier into a scenic playground of human ingenuity.", null, null, null)
            )
        ));

        // Story 12: golden-chariot-deccan-odyssey-heritage-rails
        articles.add(new BlogArticleDTO(
            "12",
            "golden-chariot-deccan-odyssey-heritage-rails",
            "The Golden Chariot & Deccan Odyssey: Royal Heritage Rails of South & West",
            "Journey through the stone temples of Hampi, the Ajanta caves, and golden Konkan beaches on India’s southern and western luxury cruisers.",
            "Heritage & History",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e0/The_Golden_Chariot.jpg/960px-The_Golden_Chariot.jpg",
            "Deccan Rail Heritage Trust",
            "Dravidian Heritage Consultant",
            7,
            "2026-10-23",
            false,
            List.of("Golden Chariot", "Deccan Odyssey", "Hampi", "Badami", "Ajanta Ellora", "Karnataka", "Maharashtra"),
            List.of(new BlogArticleDTO.QuickFact("Golden Chariot Circuit", "Bengaluru - Bandipur - Mysuru - Hampi - Goa", "map"), new BlogArticleDTO.QuickFact("Deccan Odyssey Circuit", "Mumbai - Vadodara - Udaipur - Jodhpur - Delhi", "compass"), new BlogArticleDTO.QuickFact("Theme", "Dravidian Temples & Maratha Fortresses", "landmark"), new BlogArticleDTO.QuickFact("Operated By", "KSTDC / MTDC with Indian Railways", "briefcase")),
            List.of(new BlogArticleDTO.RelatedTrain("16591", "Hampi Express", "UBL to MYS"), new BlogArticleDTO.RelatedTrain("12051", "Madgaon Jan Shatabdi", "CSMT to MAO")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. The Golden Chariot: Pride of the South", "1. The Golden Chariot: Pride of the South", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Named after the iconic stone chariot of the Vijayanagara Empire at Hampi, The Golden Chariot is South India's premier luxury train. Its purple and gold coaches wind through Karnataka, Tamil Nadu, Kerala, and Goa, showcasing UNESCO World Heritage monuments, royal Mysore palaces, and coffee-scented Western Ghats hills.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/Golden_Chariot_%287016259021%29.jpg/960px-Golden_Chariot_%287016259021%29.jpg", "The distinctive purple and gold livery of The Golden Chariot awaiting guests at the platform.", "The distinctive purple and gold livery of The Golden Chariot awaiting guests at the platform."),
                new BlogArticleDTO.ArticleSection("heading", "2. Royal Dravidian Decor & Ayurvedic Spas", "2. Royal Dravidian Decor & Ayurvedic Spas", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Each coach of The Golden Chariot is carved with motifs inspired by the Hoysala, Chalukya, and Vijayanagara architectural styles. It is one of the few trains in the world featuring a dedicated onboard Ayurvedic spa and fitness salon, where travelers can receive authentic Kerala massages as the train glides through coconut groves.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/62/The_Deccan_Odyssey_%282855799793%29.jpg/960px-The_Deccan_Odyssey_%282855799793%29.jpg", "The royal blue Deccan Odyssey cruising through the craggy plateau of Maharashtra.", "The royal blue Deccan Odyssey cruising through the craggy plateau of Maharashtra."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: The Hampi excursion halts at Hospet Junction. Make sure to visit the Vittala Temple complex at sunrise when the morning sun lights up the stone chariot's musical pillars.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. The Deccan Odyssey: Blue Pearl of the West", "3. The Deccan Odyssey: Blue Pearl of the West", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Sporting a shimmering sapphire-blue livery, the Deccan Odyssey was launched to open up the historic treasures of Maharashtra and the Deccan Plateau. Its journeys encompass the rock-cut Buddhist and Jain cave sanctuaries of Ajanta and Ellora, the wine valleys of Nashik, and the vibrant coastal beaches of Goa.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/07/The_Deccan_Odyssey_%282854969245%29.jpg/960px-The_Deccan_Odyssey_%282854969245%29.jpg", "Deccan Odyssey trainset standing majestically at CSMT Mumbai platform.", "Deccan Odyssey trainset standing majestically at CSMT Mumbai platform."),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/75/Golden_Chariot_%286869408668%29.jpg/960px-Golden_Chariot_%286869408668%29.jpg", "The Madira lounge bar car where guests unwind with classical music after excursions.", "The Madira lounge bar car where guests unwind with classical music after excursions."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? Each of the 11 guest saloons on The Golden Chariot is named after a dynasty that ruled Southern India: Kadamba, Ganga, Chalukya, Hoysala, Rashtrakuta, Vijayanagara, Bahamani, and Wodeyar.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "To experience South and Western India from the Golden Chariot and Deccan Odyssey is to view thousands of years of art and architecture through a moving golden frame.", null, null, null)
            )
        ));

        // Story 13: epic-history-indian-railways-1853-to-future
        articles.add(new BlogArticleDTO(
            "13",
            "epic-history-indian-railways-1853-to-future",
            "1853 to Present: The Epic Chronicle & Complete History of Indian Railways",
            "From the maiden 34 km journey between Bori Bunder and Thane to the world’s fourth-largest rail network and 100% green electrification.",
            "Heritage & History",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d5/1853_First_train_in_India.jpg/960px-1853_First_train_in_India.jpg",
            "Indian Railways Historical Society",
            "Chief Archivist",
            10,
            "2026-10-29",
            true,
            List.of("Railway History", "1853", "Bori Bunder", "Fairy Queen", "Steam Era", "Electrification", "Nationalization"),
            List.of(new BlogArticleDTO.QuickFact("Maiden Run Date", "16 April 1853 (3:35 PM)", "calendar"), new BlogArticleDTO.QuickFact("First Route", "Bori Bunder to Thane (34 km, 21 miles)", "map-pin"), new BlogArticleDTO.QuickFact("First Locomotives", "Sindh, Sultan, and Sahib", "zap"), new BlogArticleDTO.QuickFact("Network Today", "68,000+ route km (13,000+ trains daily)", "globe")),
            List.of(new BlogArticleDTO.RelatedTrain("12138", "Punjab Mail (Running since 1912)", "FZR to CSMT"), new BlogArticleDTO.RelatedTrain("12301", "Howrah Rajdhani (First Rajdhani 1969)", "HWH to NDLS")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. 16 April 1853: The Day India Began to Roll", "1. 16 April 1853: The Day India Began to Roll", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "At precisely 3:35 PM on Saturday, 16 April 1853, a 21-gun salute boomed across Bombay harbor. A 14-carriage train carrying 400 distinguished guests was hauled out of Bori Bunder station by three steam locomotives — Sindh, Sultan, and Sahib. Over the next 57 minutes, the train crossed 34 kilometers to Thane, inaugurating the railway age across the Asian continent.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c8/The_Fairy_Queen%2C_National_Rail_Museum%2C_New_Delhi.jpg/960px-The_Fairy_Queen%2C_National_Rail_Museum%2C_New_Delhi.jpg", "The legendary 1855 'Fairy Queen' steam locomotive (EIR-22), certified by Guinness as the world's oldest working steam engine.", "The legendary 1855 'Fairy Queen' steam locomotive (EIR-22), certified by Guinness as the world's oldest working steam engine."),
                new BlogArticleDTO.ArticleSection("heading", "2. The Fairy Queen & The Steam Age (1855-1970)", "2. The Fairy Queen & The Steam Age (1855-1970)", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Built in Leeds, England by Kitson, Thompson and Hewitson in 1855, the iconic 'Fairy Queen' hauled troop and passenger trains for the East Indian Railway. By the early 20th century, hundreds of private railway companies crisscrossed India: the Great Indian Peninsula Railway (GIPR), East Indian Railway (EIR), Madras and Southern Mahratta Railway (MSMR), and Bengal Nagpur Railway (BNR).", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8a/Victoria_Terminus_under_construction%2C_Bombay.jpg/960px-Victoria_Terminus_under_construction%2C_Bombay.jpg", "Victoria Terminus (now CSMT) under construction in the 1880s by architect F.W. Stevens.", "Victoria Terminus (now CSMT) under construction in the 1880s by architect F.W. Stevens."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: Visit the National Rail Museum in Chanakyapuri, New Delhi, to walk through the original Maharaja saloons, step aboard the 1855 Fairy Queen, and experience 170 years of working steam exhibits.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. Nationalization, Project Unigauge & Modern Electrification", "3. Nationalization, Project Unigauge & Modern Electrification", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "In 1951, the 42 separate railway systems were unified and nationalized into a single public entity: Indian Railways. Over the subsequent decades, 'Project Unigauge' converted thousands of kilometers of fragmented meter- and narrow-gauge tracks into high-capacity Broad Gauge (1,676 mm). Today, Indian Railways is achieving 100% Broad Gauge electric traction — the greenest mega-rail network on Earth.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/67/A_map_of_the_Great_Indian_Peninsula_Railway%2C_1871.jpg/960px-A_map_of_the_Great_Indian_Peninsula_Railway%2C_1871.jpg", "Historical 1871 railway map illustrating the early trunk lines linking Bombay, Calcutta, and Madras.", "Historical 1871 railway map illustrating the early trunk lines linking Bombay, Calcutta, and Madras."),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4b/EIR_22_%27Fairy_Queen%27_at_Rewari_Steam_Loco_Shed.jpg/960px-EIR_22_%27Fairy_Queen%27_at_Rewari_Steam_Loco_Shed.jpg", "The Fairy Queen preserved in steam-ready operational glory at the historic Rewari Steam Locomotive Shed.", "The Fairy Queen preserved in steam-ready operational glory at the historic Rewari Steam Locomotive Shed."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? Indian Railways is the single largest civilian employer in the world, maintaining over 1.2 million personnel, 13,000 passenger trains, and transporting over 24 million passengers every single day.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "The railways did not just carry passengers and freight across India; they forged the geographical, cultural, and economic unity of a nation.", null, null, null)
            )
        ));

        // Story 14: matheran-hill-railway-forest-toy-train
        articles.add(new BlogArticleDTO(
            "14",
            "matheran-hill-railway-forest-toy-train",
            "Matheran Hill Railway: Asia’s Automobile-Free Forest Toy Train",
            "Ascend 281 sharp horseshoe curves and dense Western Ghats monsoon jungle into India’s only vehicle-free hill station.",
            "Scenic Routes",
            "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/85/Matheran_toy_train.JPG/960px-Matheran_toy_train.JPG",
            "Western Ghats Heritage Group",
            "Sahyadri Heritage Chronicler",
            6,
            "2026-10-27",
            false,
            List.of("Matheran", "Toy Train", "Western Ghats", "Vehicle Free", "Heritage Rail", "Narrow Gauge"),
            List.of(new BlogArticleDTO.QuickFact("Gauge", "2 feet (610 mm narrow gauge)", "ruler"), new BlogArticleDTO.QuickFact("Route Length", "21 km from Neral to Matheran", "map"), new BlogArticleDTO.QuickFact("Curves", "281 curves (sharpest radius 18 m)", "corner-up-right"), new BlogArticleDTO.QuickFact("Eco Rule", "Asia's only automobile-free hill station", "shield")),
            List.of(new BlogArticleDTO.RelatedTrain("52101", "Neral - Matheran NG Passenger", "NRL to MAE"), new BlogArticleDTO.RelatedTrain("52103", "Aman Lodge - Matheran Shuttle", "AMNA to MAE")),
            List.of(
                new BlogArticleDTO.ArticleSection("heading", "1. The Vision of Sir Adamjee Peerbhoy", "1. The Vision of Sir Adamjee Peerbhoy", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Constructed between 1901 and 1907 by the philanthropist and merchant Sir Adamjee Peerbhoy at his own private expense of ₹16 lakh, the 21-kilometer Matheran Hill Railway connects Neral Junction on the Central Railway mainline to the car-free mountain ridge of Matheran in the Sahyadri mountains.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/52/Aman_Lodge_Railway_Station.jpg/960px-Aman_Lodge_Railway_Station.jpg", "The scenic Aman Lodge station, serving as the shuttle gateway where all road vehicles must stop.", "The scenic Aman Lodge station, serving as the shuttle gateway where all road vehicles must stop."),
                new BlogArticleDTO.ArticleSection("heading", "2. Negotiating 281 Curves & The One-Kiss Tunnel", "2. Negotiating 281 Curves & The One-Kiss Tunnel", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Climbing 760 meters in 21 kilometers, the toy train winds through 281 curves, some with an ultra-tight turning radius of just 18 meters. Along the climb, it passes through the romantic 'One-Kiss Tunnel' — so named because it is only 40 meters long, lasting just long enough for couples to share a quick kiss before daylight returns.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/33/Matheran_Hill_Railway_in_Western_Ghats.jpg/960px-Matheran_Hill_Railway_in_Western_Ghats.jpg", "Toy train winding along cliffside tracks overlooking dense monsoon jungle valleys.", "Toy train winding along cliffside tracks overlooking dense monsoon jungle valleys."),
                new BlogArticleDTO.ArticleSection("tip", null, "PRO TRAVELER TIP: During heavy monsoon months when the lower Neral section is paused for landslide protection, Indian Railways operates the popular Aman Lodge - Matheran hilltop shuttle train every 45 minutes.", null, null, null),
                new BlogArticleDTO.ArticleSection("heading", "3. The Silence of a Red-Clay Paradise", "3. The Silence of a Red-Clay Paradise", null, null, null),
                new BlogArticleDTO.ArticleSection("paragraph", null, "Because all automobiles are strictly banned within Matheran's municipal limits by Supreme Court environmental protection orders, the only sounds greeting arriving passengers are the clip-clop of horses, chirping bird calls, and the distant musical whistle of the toy train engine echoing over red-earth mountain trails.", null, null, null),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/77/Toy_Train_Matheran.jpg/960px-Toy_Train_Matheran.jpg", "The charming red and blue coaches resting under shade trees at Matheran terminus.", "The charming red and blue coaches resting under shade trees at Matheran terminus."),
                new BlogArticleDTO.ArticleSection("image", null, null, "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2f/Neral_Matheran_toy_train_engine.jpg/960px-Neral_Matheran_toy_train_engine.jpg", "Narrow gauge diesel engine built specifically to negotiate the sharp 18-meter mountain curves.", "Narrow gauge diesel engine built specifically to negotiate the sharp 18-meter mountain curves."),
                new BlogArticleDTO.ArticleSection("trivia", null, "DID YOU KNOW? Sir Adamjee Peerbhoy's son Abdul Hussein supervised the construction by walking the rugged cliffs on foot every week and imported specialized rails from Germany to handle the intense mountain friction.", null, null, null),
                new BlogArticleDTO.ArticleSection("quote", null, "In a world hurried by jet engines and superhighways, Matheran's toy train preserves the serene, unhurried poetry of the forest.", null, null, null)
            )
        ));

    }

    @Override
    public List<BlogArticleDTO> getAllArticles(String category, String query) {
        return articles.stream()
                .filter(a -> {
                    if (category != null && !category.isBlank() && !category.equalsIgnoreCase("All")) {
                        return a.category().equalsIgnoreCase(category.trim());
                    }
                    return true;
                })
                .filter(a -> {
                    if (query != null && !query.isBlank()) {
                        String q = query.toLowerCase().trim();
                        return a.title().toLowerCase().contains(q)
                                || a.subtitle().toLowerCase().contains(q)
                                || a.tags().stream().anyMatch(t -> t.toLowerCase().contains(q))
                                || a.category().toLowerCase().contains(q);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BlogArticleDTO> getArticleBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return articles.stream()
                .filter(a -> a.slug().equalsIgnoreCase(slug.trim()))
                .findFirst();
    }

    @Override
    public List<BlogArticleDTO> getFeaturedArticles() {
        return articles.stream().filter(a -> Boolean.TRUE.equals(a.featured())).limit(3).collect(Collectors.toList());
    }

    @Override
    public List<String> getCategories() {
        return List.of("Scenic Routes", "Train Tech", "Heritage & History", "Travel Tips", "Food & Culture");
    }

    private void initTips() {
        List<BlogTipDTO> konkanTips = new CopyOnWriteArrayList<>();
        konkanTips.add(new BlogTipDTO(UUID.randomUUID().toString(), "konkan-railway-scenic-odyssey", "Rohit Shenoy", "@konkan_railfan", "SCENIC_SPOT", "Sit on the right side of the train when traveling from Mumbai to Goa for unobstructed views of Dudhsagar Falls and coastal river estuaries! Book non-AC 2S or 3E for lower window reflections.", null, "September 2026", 48, LocalDateTime.now().minusDays(5)));
        konkanTips.add(new BlogTipDTO(UUID.randomUUID().toString(), "konkan-railway-scenic-odyssey", "Ananya Deshmukh", "@travel_with_anu", "FOOD_TIP", "At Chiplun and Ratnagiri stations, don't miss the local jackfruit chips and fresh Alphonso mango pulp bottles sold on Platform 1.", null, "October 2026", 31, LocalDateTime.now().minusDays(2)));
        tipsMap.put("konkan-railway-scenic-odyssey", konkanTips);

        List<BlogTipDTO> vbTips = new CopyOnWriteArrayList<>();
        vbTips.add(new BlogTipDTO(UUID.randomUUID().toString(), "vande-bharat-evolution-future-sleeper", "Vikramaditya Roy", "@vande_express", "BERTH_ADVICE", "The Executive Class (EC) revolving seats can be rotated 180 degrees to face the large panoramic picture window — perfect during Western Ghats or Shivalik climbs!", null, "October 2026", 52, LocalDateTime.now().minusDays(3)));
        tipsMap.put("vande-bharat-evolution-future-sleeper", vbTips);

        List<BlogTipDTO> pambanTips = new CopyOnWriteArrayList<>();
        pambanTips.add(new BlogTipDTO(UUID.randomUUID().toString(), "pamban-sea-bridge-engineering-marvel", "K. Ramanathan", "@rameswaram_tracks", "PHOTOGRAPHY", "For the classic cantilever bridge photo, stand near the train door between Mandapam and Pamban (at safe distance) during early morning sunrise when the sea waves turn turquoise.", null, "August 2026", 64, LocalDateTime.now().minusDays(10)));
        tipsMap.put("pamban-sea-bridge-engineering-marvel", pambanTips);

        List<BlogTipDTO> ksTips = new CopyOnWriteArrayList<>();
        ksTips.add(new BlogTipDTO(UUID.randomUUID().toString(), "kalka-shimla-railway-himalayan-queen", "Capt. H. S. Brar", "@himalayan_tracks", "SCENIC_SPOT", "Tunnel 33 at Barog is the most atmospheric stop. The train halts for 10 minutes — step down on Platform 1 for warm parathas and ginger chai surrounded by towering pine trees.", null, "October 2026", 42, LocalDateTime.now().minusDays(4)));
        tipsMap.put("kalka-shimla-railway-himalayan-queen", ksTips);

        List<BlogTipDTO> tatkalTips = new CopyOnWriteArrayList<>();
        tatkalTips.add(new BlogTipDTO(UUID.randomUUID().toString(), "mastering-tatkal-irctc-ticket-booking", "Priya Sharma", "@rail_tactics", "BOOKING_TACTIC", "Always pre-fill the IRCTC Master Passenger List in your profile 24 hours prior. During the 10:00 AM AC rush, selecting from the Master List saves 40 critical seconds!", null, "October 2026", 89, LocalDateTime.now().minusDays(1)));
        tipsMap.put("mastering-tatkal-irctc-ticket-booking", tatkalTips);
    }

    @Override
    public List<BlogTipDTO> getTipsForArticle(String slug) {
        return tipsMap.computeIfAbsent(slug.toLowerCase().trim(), k -> new CopyOnWriteArrayList<>());
    }

    @Override
    public BlogTipDTO addTip(String slug, BlogTipDTO tipDTO) {
        String id = UUID.randomUUID().toString();
        BlogTipDTO newTip = new BlogTipDTO(
            id,
            slug.toLowerCase().trim(),
            tipDTO.authorName() != null && !tipDTO.authorName().isBlank() ? tipDTO.authorName().trim() : "Rail Passenger",
            tipDTO.authorHandle() != null && !tipDTO.authorHandle().isBlank() ? tipDTO.authorHandle().trim() : "@passenger",
            tipDTO.category() != null ? tipDTO.category() : "GENERAL",
            tipDTO.tipText() != null ? tipDTO.tipText().trim() : "",
            tipDTO.photoUrl(),
            tipDTO.travelDate() != null ? tipDTO.travelDate() : "Recent Journey",
            0,
            LocalDateTime.now()
        );
        tipsMap.computeIfAbsent(slug.toLowerCase().trim(), k -> new CopyOnWriteArrayList<>()).add(0, newTip);
        return newTip;
    }

    @Override
    public BlogTipDTO likeTip(String slug, String tipId) {
        List<BlogTipDTO> list = tipsMap.get(slug.toLowerCase().trim());
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                BlogTipDTO t = list.get(i);
                if (t.id().equals(tipId)) {
                    BlogTipDTO updated = new BlogTipDTO(
                        t.id(), t.articleSlug(), t.authorName(), t.authorHandle(),
                        t.category(), t.tipText(), t.photoUrl(), t.travelDate(),
                        t.likesCount() + 1, t.createdAt()
                    );
                    list.set(i, updated);
                    return updated;
                }
            }
        }
        return null;
    }
}
