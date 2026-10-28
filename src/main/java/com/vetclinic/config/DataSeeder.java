package com.vetclinic.config;

import com.vetclinic.adoption.AdoptionService;
import com.vetclinic.adoption.dto.AdoptionActionRequest;
import com.vetclinic.adoption.dto.AdoptionResponse;
import com.vetclinic.adoption.dto.CreateAdoptionRequest;
import com.vetclinic.appointment.AppointmentService;
import com.vetclinic.appointment.dto.AppointmentRequest;
import com.vetclinic.appointment.dto.AppointmentResponse;
import com.vetclinic.auth.Role;
import com.vetclinic.auth.UserService;
import com.vetclinic.food.FoodCategory;
import com.vetclinic.food.FoodProductService;
import com.vetclinic.food.dto.FoodProductRequest;
import com.vetclinic.food.dto.FoodProductResponse;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.owner.OwnerService;
import com.vetclinic.owner.dto.OwnerRequest;
import com.vetclinic.owner.dto.OwnerResponse;
import com.vetclinic.pet.PetService;
import com.vetclinic.pet.PetSex;
import com.vetclinic.pet.Species;
import com.vetclinic.pet.dto.PetRequest;
import com.vetclinic.pet.dto.PetResponse;
import com.vetclinic.sale.FoodSaleService;
import com.vetclinic.sale.PaymentMethod;
import com.vetclinic.sale.dto.CreateSaleRequest;
import com.vetclinic.sale.dto.CreateSaleRequest.SaleLineRequest;
import com.vetclinic.sale.dto.SaleResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

/**
 * Populates a fresh database with a small demo data set so the API is usable straight after start up.
 * Disable it with {@code app.seed.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    ApplicationRunner seedDemoData(OwnerService ownerService,
                                   OwnerRepository ownerRepository,
                                   PetService petService,
                                   FoodProductService foodProductService,
                                   FoodSaleService foodSaleService,
                                   AdoptionService adoptionService,
                                   AppointmentService appointmentService,
                                   UserService userService) {
        return args -> {
            boolean hasOwners = ownerService.findAll(Pageable.ofSize(1)).hasContent();
            if (hasOwners) {
                log.info("Seed data skipped for clinic records, the database already has owners");
            } else {
                seedClinicData(ownerService, petService, foodProductService, foodSaleService,
                        adoptionService, appointmentService);
            }
            seedUsers(userService, ownerRepository);
        };
    }

    private void seedClinicData(OwnerService ownerService,
                                PetService petService,
                                FoodProductService foodProductService,
                                FoodSaleService foodSaleService,
                                AdoptionService adoptionService,
                                AppointmentService appointmentService) {
        OwnerResponse lucia = createOwner(ownerService, "Lucia Fernandez", "lucia.fernandez@example.com",
                "+34 600 111 222", "Calle Sol 4, Madrid");
        OwnerResponse carlos = createOwner(ownerService, "Carlos Gil", "carlos.gil@example.com",
                "+34 600 333 444", "Avenida del Mar 21, Valencia");

        FoodProductResponse adultDogFood = createProduct(foodProductService, "Crocodile Adult Mini",
                "Royal Canin", FoodCategory.DOG_FOOD, "3 kg bag for small adult dogs",
                new BigDecimal("24.50"), 40, new BigDecimal("3.000"));
        FoodProductResponse kittenFood = createProduct(foodProductService, "Kitten Sterilised 1.5 kg",
                "Purina Pro Plan", FoodCategory.KITTEN_FOOD, "Chicken and rice, 1.5 kg bag",
                new BigDecimal("12.90"), 60, new BigDecimal("1.500"));
        FoodProductResponse dentalTreats = createProduct(foodProductService, "Dental Sticks x14",
                "Greenies", FoodCategory.TREATS, "Daily dental chews, 14 units",
                new BigDecimal("8.75"), 12, new BigDecimal("0.280"));
        createProduct(foodProductService, "Senior Cat 2 kg", "Hill's Science Diet",
                FoodCategory.CAT_FOOD, "Low phosphorus formula, 2 kg bag",
                new BigDecimal("19.40"), 0, new BigDecimal("2.000"));

        PetResponse roco = petService.create(new PetRequest("Roco", Species.DOG, "Schnauzer",
                LocalDate.now().minusYears(4), PetSex.MALE, null, null,
                "Shy with strangers, vaccinated and microchipped."));
        PetResponse nina = petService.create(new PetRequest("Nina", Species.CAT, "European Shorthair",
                LocalDate.now().minusYears(2), PetSex.FEMALE, null, null,
                "Looking for a calm home without small children."));
        petService.create(new PetRequest("Bruno", Species.DOG, "Beagle", LocalDate.now().minusYears(3),
                PetSex.MALE, null, lucia.id(), "Regular check-ups, next visit due in autumn."));
        petService.create(new PetRequest("Luna", Species.CAT, "Siamese", LocalDate.now().minusMonths(6),
                PetSex.FEMALE, null, lucia.id(), "New kitten, first vaccine pending."));

        appointmentService.create(new AppointmentRequest(nina.id(),
                LocalDateTime.now().plusDays(3).withHour(10).withMinute(30).withSecond(0).withNano(0),
                "Vaccination and general check-up", "Dra. Elena Vazquez", 30,
                "Bring the previous vaccination record."));
        appointmentService.create(new AppointmentRequest(roco.id(),
                LocalDateTime.now().plusDays(7).withHour(17).withMinute(0).withSecond(0).withNano(0),
                "Deworming and weight control", "Dr. Pablo Nieto", 45, null));

        AdoptionResponse adoption = adoptionService.adopt(new CreateAdoptionRequest(
                roco.id(), carlos.id(), null, new BigDecimal("85.00"), "Picked up on Saturday morning."));
        adoptionService.markAsReturned(adoption.id(),
                new AdoptionActionRequest("Not compatible with the adopter's garden, back to the shelter."));

        SaleResponse sale = foodSaleService.registerSale(new CreateSaleRequest(
                lucia.id(), "Lucia Fernandez", PaymentMethod.CARD, null,
                List.of(new SaleLineRequest(adultDogFood.id(), 2),
                        new SaleLineRequest(kittenFood.id(), 1),
                        new SaleLineRequest(dentalTreats.id(), 3)),
                "Demo ticket created by the seeder."));
        log.info("Seeded demo data: 2 owners, 4 products, 4 pets, 2 appointments, "
                + "1 adoption, 1 sale {} for {} EUR", sale.saleNumber(), sale.totalAmount());
    }

    /**
     * Demo accounts, one per role. The client account is linked to Lucia's owner record so the portal has
     * data on first login. Seeding is skipped when any account already exists.
     */
    private void seedUsers(UserService userService, OwnerRepository ownerRepository) {
        if (userService.count() > 0) {
            return;
        }
        userService.createAccount("Administrador", "admin@vetclinic.example", "Admin1234!", Role.ADMIN, null);
        userService.createAccount("Gestor de clinica", "gestor@vetclinic.example", "Gestor1234!",
                Role.MANAGER, null);
        ownerRepository.findByEmailIgnoreCase("lucia.fernandez@example.com").ifPresentOrElse(
                lucia -> {
                    userService.createAccount("Lucia Fernandez", "cliente@vetclinic.example", "Cliente1234!",
                            Role.CLIENT, lucia);
                    log.info("Seeded demo accounts: admin@vetclinic.example, gestor@vetclinic.example, "
                            + "cliente@vetclinic.example");
                },
                () -> log.warn("Seeded staff accounts only: no Lucia owner found for the demo client account"));
    }

    private OwnerResponse createOwner(OwnerService ownerService, String name, String email, String phone,
                                      String address) {
        return ownerService.create(new OwnerRequest(name, email, phone, address));
    }

    private FoodProductResponse createProduct(FoodProductService service, String name, String brand,
                                              FoodCategory category, String description, BigDecimal price,
                                              int stock, BigDecimal weight) {
        return service.create(new FoodProductRequest(name, brand, category, description, price, stock, weight));
    }
}
