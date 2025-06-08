#include <string>
#include <vector>
#include "../include/Order.h"
#include "../include/Volunteer.h"
#include"../include/Customer.h"
#include"../include/WareHouse.h"
#include"../include/Action.h"
#include<iostream>
using namespace std;
#include<fstream>
#include<sstream>
#include <algorithm>



class BaseAction;
class Volunteer;







// Warehouse responsible for Volunteers, Customers Actions, and Orders.

        //move assignment operator
        WareHouse& WareHouse::operator=(WareHouse&& other) noexcept{
            std::swap(isOpen, other.isOpen);
            std::swap(isBackedUp, other.isBackedUp);
            std::swap(actionsLog, other.actionsLog);
            std::swap(volunteers, other.volunteers);
            std::swap(pendingOrders, other.pendingOrders);
            // Similar swapping for other vectors...
            std::swap(customerCounter, other.customerCounter);
            std::swap(volunteerCounter, other.volunteerCounter);
            std::swap(orderCounter, other.orderCounter);
            return *this;
        }

        //move constructor
        WareHouse::WareHouse(WareHouse&& other) noexcept
        :   isOpen(std::move(other.isOpen)), isBackedUp(std::move(other.isBackedUp)),
            actionsLog(std::move(other.actionsLog)), volunteers(std::move(other.volunteers)),
            pendingOrders(std::move(other.pendingOrders)), inProcessOrders(std::move(other.inProcessOrders)),
            completedOrders(std::move(other.completedOrders)), customers(std::move(other.customers)),
            customerCounter(std::move(other.customerCounter)),
            volunteerCounter(std::move(other.volunteerCounter)),
            orderCounter(std::move(other.orderCounter)){
            // Reset the moved object's state
            other.isOpen = false;
            other.isBackedUp = false;
            other.customerCounter = 0;
            other.volunteerCounter = 0;
            other.orderCounter = 0;
            }
        
        //destructor
        WareHouse::~WareHouse(){
            for (BaseAction* action : actionsLog){
                delete action;
            }
            for (Volunteer* volunteer : volunteers){
                delete volunteer;
            }
            

            for (Order* order : pendingOrders){
                delete order;
            }

            for (Order* order : inProcessOrders){
                delete order;
            }

            for (Order* order : completedOrders){
                delete order;
            }

            for (Customer* customer : customers){
                delete customer;
            }
            
            actionsLog.clear();
            volunteers.clear();
            
            customers.clear();
            pendingOrders.clear();
            inProcessOrders.clear();
            completedOrders.clear();
            
        }

        //copy assignment operator
    WareHouse& WareHouse::operator=(const WareHouse& other){
        if(this!=&other){
            for (BaseAction* action : this->actionsLog){
                delete action;
            }
            
            for (Volunteer* volunteer : this->volunteers){
                delete volunteer;
            }
           

            for (Order* order : pendingOrders){
                delete order;
            }

            for (Order* order : inProcessOrders){
                delete order;
            }

            for (Order* order : completedOrders){
                delete order;
            }

            for (Customer* customer : customers){
                delete customer;
            }
            
            actionsLog.clear();
            volunteers.clear();
            
            customers.clear();
            pendingOrders.clear();
            inProcessOrders.clear();
            completedOrders.clear();
            

            isOpen = other.isOpen;
            customerCounter = other.customerCounter;
            orderCounter = other.orderCounter;
            volunteerCounter = other.volunteerCounter;
            for (BaseAction* action : other.actionsLog){
                
                actionsLog.push_back(action->clone());
            }
            for (Volunteer* volunteer : other.volunteers){
                volunteers.push_back(volunteer->clone());
            }
           

            for(Order* order : other.pendingOrders){
                pendingOrders.push_back(new Order(*order));
            }

            for(Order* order: other.inProcessOrders){
                inProcessOrders.push_back(new Order(*order));
            }

            for(Order* order: other.completedOrders){
                completedOrders.push_back(new Order(*order));
            }

            for (Customer* customer : other.customers){
                customers.push_back(customer->clone());
            }
        }
            return *this;
        }

        //copy constructor
        WareHouse::WareHouse(const WareHouse& other) 
        : isOpen(other.isOpen), isBackedUp(other.isBackedUp),
        actionsLog(),volunteers(),pendingOrders(),inProcessOrders(),
        completedOrders(),customers(),customerCounter(other.customerCounter), 
        volunteerCounter(other.volunteerCounter),orderCounter(other.orderCounter) {
        // Perform a deep copy of vectors
        // You may need to provide copy constructors for each class (Volunteer, Order, etc.)
        if(this!=&other){
        for (BaseAction* action : other.actionsLog) {
            actionsLog.push_back(action->clone());
        }
        
         for (Volunteer* volunteer : other.volunteers) {
            volunteers.push_back(volunteer->clone());
        }
        for (Order* order : other.pendingOrders) {
            pendingOrders.push_back(new Order(*order));
        }
        for (Order* order : other.inProcessOrders) {
            inProcessOrders.push_back(new Order(*order));
         }
        for (Order* order : other.completedOrders) {
            completedOrders.push_back(new Order(*order));
        }
        for (Customer* customer : other.customers) {
            customers.push_back(customer->clone());
        }
        }
    }
        
        WareHouse:: WareHouse(const string &configFilePath)
        : isOpen(false), isBackedUp(false), actionsLog(), volunteers(), pendingOrders(),
          inProcessOrders(), completedOrders(), customers(),
          customerCounter(0), volunteerCounter(0), orderCounter(0)
        {
            std::string line;
            std::ifstream inputFile(configFilePath);
            while (std::getline(inputFile, line)) 
            {
                std::istringstream iss(line);
                std::vector<std::string> words;
                std::string word;

                while (iss >> word) {
                    words.push_back(word);
                }
                
                if(words[0] == "customer")
                {
                    Customer* customer;
                    string name = words[1];
                    string customerType = words[2];
                    int distance = std::stoi(words[3]);
                    int maxOrders = std::stoi(words[4]);
                    if(customerType == "soldier")
                    {
                        customer = new SoldierCustomer(customerCounter, name, distance, maxOrders);
                    }else{
                        customer = new CivilianCustomer(customerCounter, name, distance, maxOrders);
                    }

                    this->addCustomer(customer);
                }

                if(words[0] == "volunteer")
                {
                    Volunteer* volunteer;
                    string name = words[1];
                    string volunteerType = words[2];
                    
                    if(volunteerType == "collector")
                    {
                        int coolDown = std::stoi(words[3]);
                        volunteer = new CollectorVolunteer(volunteerCounter, name, coolDown);
                    }
                    if(volunteerType == "limited_collector")
                    {
                        int coolDown = std::stoi(words[3]);
                        int maxOrders = std::stoi(words[4]);

                        volunteer = new LimitedCollectorVolunteer(volunteerCounter, name, coolDown, maxOrders);
                    }

                    if(volunteerType == "driver")
                    {
                        int maxDistance = std::stoi(words[3]);
                        int distancePerStep = std::stoi(words[4]);

                        volunteer = new DriverVolunteer(volunteerCounter, name, maxDistance, distancePerStep);
                    }

                    if(volunteerType == "limited_driver")
                    {
                        int maxDistance = std::stoi(words[3]);
                        int distancePerStep = std::stoi(words[4]);
                        int maxOrders = std::stoi(words[5]);

                        volunteer = new LimitedDriverVolunteer(volunteerCounter, name, maxDistance, distancePerStep, maxOrders);
                    }

                    this->volunteers.push_back(volunteer);
                    volunteerCounter++;
                }
            }
            inputFile.close();
            
        }
        void WareHouse::start()
        {
            open();
            cout<< "WareHouse is open!" << endl;
            string command;
            while(isOpen)
            {
                std::getline(std::cin, command);
                std::istringstream iss(command);
                std::vector<std::string> words;
                std::string word;

                while (iss >> word) {
                    words.push_back(word);
                }
                if(words[0] == "order")
                {
                    int customerId = std::stoi(words[1]); 
                    BaseAction* a = new AddOrder(customerId);
                    a->act(*this);
                }
                if(words[0] == "customer")
                {
                    string name = words[1];
                    string customerType = words[2];
                    int distance = std::stoi(words[3]);
                    int maxOrders = std::stoi(words[4]);
                    BaseAction* a = new AddCustomer(name, customerType, distance, maxOrders);
                    a->act(*this);
                    
                }
                if(words[0] == "orderStatus")
                {
                    int orderId = std::stoi(words[1]);
                    BaseAction* a = new PrintOrderStatus(orderId);
                    a->act(*this);
                }
                if(words[0] == "customerStatus")
                {
                    int customerId = std::stoi(words[1]);
                    BaseAction* a = new PrintCustomerStatus(customerId);
                    a->act(*this);
                }
                if(words[0] == "volunteerStatus")
                {
                    int volunteerId = std::stoi(words[1]);
                    BaseAction* a = new PrintVolunteerStatus(volunteerId);
                    a->act(*this);
                }
                if(words[0] == "log")
                {
                    BaseAction* a = new PrintActionsLog;
                    a->act(*this);
                }
                if(words[0] == "close")
                {
                    BaseAction* a = new Close;
                    a->act(*this);
                }

                if(words[0] == "step")
                {
                    int numOfSteps = std::stoi(words[1]);
                    BaseAction* a = new SimulateStep(numOfSteps);
                    a->act(*this);
                }

                if(words[0] == "backup"){
                    BaseAction* a = new BackupWareHouse;
                    a->act(*this);
                }

                if(words[0] == "restore"){
                    BaseAction* a = new RestoreWareHouse;
                    a->act(*this);
                }
                
                
            }
        }
        void WareHouse::addOrder(Order* order)
        {
           pendingOrders.push_back(order);
           orderCounter++;
        }
        void WareHouse::addAction(BaseAction* action)
        {
            actionsLog.push_back(action);
        }
         void WareHouse::addCustomer(Customer* customer)
        {
            customers.push_back(customer);
            customerCounter++;
        }
        Customer& WareHouse::getCustomer(int customerId) const
        {
  
                return *customers[customerId]; 
            
        }
        Volunteer& WareHouse:: getVolunteer(int volunteerId) const
        {
            for(Volunteer* volunteer: volunteers)
                {
                    if(volunteer->getId() == volunteerId)
                    {
                        return *volunteer;
                    }
                }

            return *(new CollectorVolunteer(-1, "None", -1));
            }
            
            
            
        
        
        Order& WareHouse::getOrder(int orderId) const
        {
            for(Order * order: pendingOrders)
            {
                if(orderId == order->getId())
                {
                    return *order;
                }
            }

            for(Order * order: inProcessOrders)
            {
                if(orderId == order->getId())
                {
                    return *order;
                }
            }

            for(Order * order: completedOrders)
            {
                if(orderId == order->getId())
                {
                    return *order;
                }
            }

            return *(new Order(-1, -1, -1));
        }
            
        
        const vector<BaseAction*>& WareHouse::getActions() const
        {
            return actionsLog;
        }

        const vector<Order*>& WareHouse:: getPendingOrders() const
        {
            return pendingOrders;
        }
        const vector<Order*>& WareHouse:: getInProcessOrders() const
        {
            return inProcessOrders;
        }

        const vector<Order*>& WareHouse:: getCompletedOrders() const
        {
            return completedOrders;
        }

        void WareHouse::close()
        {
            isOpen = false;

        }
        void WareHouse::open()
        {
            isOpen = true;
        }
        int WareHouse::getOrderCounter() const
        {
            return orderCounter;
        }
        int WareHouse::getCustomerCounter() const
        {
            return customerCounter;
        }

        int WareHouse::getVolunteerCounter() const
        {
            return volunteerCounter;
        }
        
        static bool compareOrders(const Order* order1,const  Order* order2) {
            return order1->getId() < order2->getId();
        }

        static void sortOrders(std::vector<Order*>& orders) {
            std::sort(orders.begin(), orders.end(), compareOrders);
    
        }

        void WareHouse::step()
        {
            
        
            sortOrders(pendingOrders);
            
            auto orderIter = pendingOrders.begin();
            while(orderIter != pendingOrders.end())
            {
                bool moveOn = true;
                Order* order = *orderIter;
                if(order->getStatus() == OrderStatus::PENDING)
                {
                    for(Volunteer* volunteer : volunteers)
                    {
                        if(volunteer->getCustType() == "Collector" || volunteer->getCustType() == "LimitedCollector")
                        {
                            if(volunteer->canTakeOrder(*order) )
                            {
        
                                volunteer->acceptOrder(*order);
                                order->setCollectorId(volunteer->getId());
                                order->setStatus(OrderStatus::COLLECTING);
                                pendingOrders.erase(pendingOrders.begin());
                                inProcessOrders.push_back(order);
                                moveOn = false;
                                break;
                            }
                            
                        }
                    }
                }else if(order->getStatus() == OrderStatus::COLLECTING)
                {
                    for(Volunteer* volunteer : volunteers)
                    {
                        if(volunteer->getCustType() == "Driver" || volunteer->getCustType() == "LimitedDriver")
                        {
                            if(volunteer->canTakeOrder(*order) )
                            {
                                volunteer->acceptOrder(*order);
                                order->setDriverId(volunteer->getId());
                                order->setStatus(OrderStatus::DELIVERING);
                                pendingOrders.erase(pendingOrders.begin() );
                                inProcessOrders.push_back(order);
                                moveOn = false;
                                break;
                            }
                            
                        }
                    } 
                }
                if(moveOn)
                    orderIter++;
            }


            for(Volunteer* volunteer : volunteers)
                {
                    if(volunteer->isBusy())
                    {
                        volunteer->step();
                        if(volunteer->getActiveOrderId() == -1)
                        {
                            int orderId = volunteer->getCompletedOrderId();
                            Order* order = &getOrder(orderId);
                            if(volunteer->getCustType() == "Collector" || volunteer->getCustType() == "LimitedCollector")
                            {
                                inProcessOrders.erase(std::remove(inProcessOrders.begin(), inProcessOrders.end(), order), inProcessOrders.end());
                                pendingOrders.push_back(order);
                            }
                            if(volunteer->getCustType() == "Driver" || volunteer->getCustType() == "LimitedDriver")
                            {
                                inProcessOrders.erase(std::remove(inProcessOrders.begin(), inProcessOrders.end(), order), inProcessOrders.end());
                                completedOrders.push_back(order);
                                order->setStatus(OrderStatus::COMPLETED);
                            }
                            if(volunteer->getCustType() == "LimitedCollector" || volunteer->getCustType() == "LimitedDriver")
                            {
                                if(!volunteer->hasOrdersLeft())
                                {
                                    delete volunteer;
                                    volunteers.erase(std::remove(volunteers.begin(), volunteers.end(), volunteer), volunteers.end());
                                    //delete volunteer;
                                    volunteerCounter--;
                                }
                            }
                        }
                    }
                }
                
        }
        

       
        void WareHouse::backUpWareHouse() 
        {
            isBackedUp = true;
            
        }

        bool WareHouse::getIsBackedUp() const
        {
            return this->isBackedUp;
        }

    // private:
    //     bool isOpen;
    //     vector<BaseAction*> actionsLog;
    //     vector<Volunteer*> volunteers;
    //     vector<Order*> pendingOrders;
    //     vector<Order*> inProcessOrders;
    //     vector<Order*> completedOrders;
    //     vector<Customer*> customers;
    //     int customerCounter; //For assigning unique customer IDs
    //     int volunteerCounter; //For assigning unique volunteer IDs
    //     int orderCounter; //For assigning unique order IDs
        






