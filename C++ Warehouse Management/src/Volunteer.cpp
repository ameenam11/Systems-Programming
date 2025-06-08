#include <string>
#include <vector>
#include "../include/Order.h"
#include "../include/Volunteer.h"
#include <iostream>
using namespace std;


using std::string;
using std::vector;

#define NO_ORDER -1

        Volunteer::Volunteer(int id, const string &name)
        : completedOrderId(-1), activeOrderId(-1), id(id), name(name)
        {}
          
        int Volunteer::getId() const
        {
            return id;
        }
        const string& Volunteer::getName() const
        {
            return name;
        }
        int Volunteer::getActiveOrderId() const
        {
            return activeOrderId;
        }
        int Volunteer::getCompletedOrderId() const
        {
            return completedOrderId;
        }
        bool Volunteer::isBusy() const // Signal whether the volunteer is currently processing an order    
        {
            return activeOrderId != NO_ORDER;
        }


///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
 ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
 ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
 

    
        CollectorVolunteer::CollectorVolunteer(int id, const string &name, int coolDown)
        :Volunteer(id,name),coolDown(coolDown), timeLeft(0)
        {}
        

        CollectorVolunteer* CollectorVolunteer::clone() const 
        {
            return new CollectorVolunteer(*this);
        }

        void CollectorVolunteer:: step() {
            bool flag = this->decreaseCoolDown();

            if(flag)
            {
                completedOrderId = activeOrderId;
                activeOrderId = NO_ORDER;
            }
        }
        int CollectorVolunteer:: getCoolDown() const{
            return this->coolDown;
        }
        int CollectorVolunteer:: getTimeLeft() const{
            return this->timeLeft;
        }
        bool CollectorVolunteer:: decreaseCoolDown(){ //Decrease timeLeft by 1,return true if timeLeft=0,false otherwise
            timeLeft = timeLeft - 1;
            return this->timeLeft == 0;
        } 
        bool CollectorVolunteer:: hasOrdersLeft() const{
            return true;
        }
        bool CollectorVolunteer:: canTakeOrder(const Order &order) const{
            return !(this->isBusy());
        }
        void CollectorVolunteer:: acceptOrder(const Order &order){
            
             activeOrderId = order.getId();
             timeLeft = coolDown;
            
        }
        string CollectorVolunteer:: toString() const{
            string result = "VolunteerID: " + std::to_string(this->getId());
            if(isBusy())
            {
                result += "\nisBusy: true";
            }else{
                result += "\nisBusy: false";
            }
            if(activeOrderId == -1){
                result += "\nOrderId: None";
            }else{
                result += "\nOrderId: " + std::to_string(activeOrderId);
            }
            if(this->getActiveOrderId() != -1)
                result += "\nTime Left: " + std::to_string(this->getTimeLeft());
            else
                result += "\nTime Left: None";
            result += "\nOrdersLeft: No Limit";
            return result;
        }

        string CollectorVolunteer::getCustType() const
        {
            return "Collector";
        }
    
  




    
        LimitedCollectorVolunteer:: LimitedCollectorVolunteer(int id, const string &name, int coolDown ,int maxOrders): 
        CollectorVolunteer(id,name, coolDown), maxOrders(maxOrders), ordersLeft(maxOrders)
        {}
         
        LimitedCollectorVolunteer*  LimitedCollectorVolunteer::clone() const 
        {
            return new LimitedCollectorVolunteer(*this);
        }

        bool LimitedCollectorVolunteer:: hasOrdersLeft() const{
            return this->ordersLeft > 0;
        }
        bool LimitedCollectorVolunteer:: canTakeOrder(const Order &order) const{
            
            return ((CollectorVolunteer::canTakeOrder(order)) && (maxOrders > 0));
        }
        void LimitedCollectorVolunteer:: acceptOrder(const Order &order) {
            CollectorVolunteer::acceptOrder(order);
            ordersLeft--;
        }

        int LimitedCollectorVolunteer:: getMaxOrders() const{
            return this->maxOrders;
        }
        int LimitedCollectorVolunteer:: getNumOrdersLeft() const{
            return ordersLeft;
        }
        string LimitedCollectorVolunteer:: toString() const{
            string result = "VolunteerID: " + std::to_string(this->getId());
            if(isBusy())
            {
                result += "\nisBusy: True";
            }else{
                result += "\nisBusy: False";
            }
            if(activeOrderId == -1){
                result += "\nOrderId: None";
            }else{
                result += "\nOrderId: " + std::to_string(activeOrderId);
            }
            if(this->getActiveOrderId() != -1)
                result += "\nTime Left: " + std::to_string(this->getTimeLeft());
            else
                result += "\nTime Left: None";
            result += "\nOrdersLeft: " + std::to_string(this->ordersLeft);
            return result;
        }

        string LimitedCollectorVolunteer::getCustType() const
        {
            return "LimitedCollector";
        }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
 ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
 ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
 

        DriverVolunteer::DriverVolunteer(int id, const string &name, int maxDistance, int distancePerStep)
        : Volunteer(id, name), maxDistance(maxDistance), distancePerStep(distancePerStep), distanceLeft(0)
        {}
         
        DriverVolunteer* DriverVolunteer::clone() const 
        {
            return new DriverVolunteer(*this);
        }

        int DriverVolunteer::getDistanceLeft() const
        {
            return distanceLeft;
        }
        int DriverVolunteer::getMaxDistance() const
        {
            return maxDistance;
        }
        int DriverVolunteer::getDistancePerStep() const
        {
            return distancePerStep;
        }  
        bool DriverVolunteer::decreaseDistanceLeft() //Decrease distanceLeft by distancePerStep,return true if distanceLeft<=0,false otherwise
        {
            if(distanceLeft<distancePerStep)
            {
                distanceLeft = 0;
            }else{
                distanceLeft = distanceLeft - distancePerStep;
            }
            return distanceLeft<=0;
        }
        bool DriverVolunteer::hasOrdersLeft() const 
        {
            return true;
        }
        bool DriverVolunteer::canTakeOrder(const Order &order) const  // Signal if the volunteer is not busy and the order is within the maxDistance
        {
            return !(isBusy()) && (maxDistance >= order.getDistance());
        }
        void DriverVolunteer::acceptOrder(const Order &order)  // Assign distanceLeft to order's distance
        {
            
            distanceLeft = order.getDistance();
            activeOrderId = order.getId();
            
        }
        void DriverVolunteer::step()  // Decrease distanceLeft by distancePerStep
        {
            bool flag = decreaseDistanceLeft();

            if(flag){
                completedOrderId = activeOrderId;
                activeOrderId = NO_ORDER;
            }
        }
        string DriverVolunteer::toString() const {
            string result = "VolunteerID: " + std::to_string(this->getId());
            if(this->isBusy()){
                result += "\nisBusy: True";    
            }else{
                result += "\nisBusy: False";
            }
            
            if(this->activeOrderId >=0)
                result += "\nOrderId: " + std::to_string(activeOrderId);
            else
                result += "\nOrderId: None";
            if(this->getActiveOrderId() != -1)
                result += "\nDistance Left: " + std::to_string(this->getDistanceLeft());
            else
                result += "\nDistance Left: None";
            result += "\nOrdersLeft: No Limit";
            return result;
        }

        string DriverVolunteer::getCustType() const
        {
            return "Driver";
        }

  




  
        LimitedDriverVolunteer::LimitedDriverVolunteer(int id, const string &name, int maxDistance, int distancePerStep,int maxOrders)
        : DriverVolunteer(id,name,maxDistance, distancePerStep), maxOrders(maxOrders), ordersLeft(maxOrders)
        {}
         
        LimitedDriverVolunteer* LimitedDriverVolunteer::clone() const 
        {
            return new LimitedDriverVolunteer(*this);
        }
        int LimitedDriverVolunteer::getMaxOrders() const
        {
            return maxOrders;
        }
        int LimitedDriverVolunteer::getNumOrdersLeft() const
        {
            return ordersLeft;
        }
        bool LimitedDriverVolunteer::hasOrdersLeft() const
        {
            return ordersLeft>0;
        }
        bool LimitedDriverVolunteer::canTakeOrder(const Order &order) const  // Signal if the volunteer is not busy, the order is within the maxDistance.
        {
            return !(isBusy()) && (getMaxDistance() >= order.getDistance()) && (maxOrders > 0);
        }
        void LimitedDriverVolunteer::acceptOrder(const Order &order)  // Assign distanceLeft to order's distance and decrease ordersLeft
        {
            DriverVolunteer::acceptOrder(order);
            ordersLeft--;
        }
        string LimitedDriverVolunteer::toString() const {
            string result = "VolunteerID: " + std::to_string(this->getId());
            if(this->isBusy()){
                result += "\nisBusy: True";    
            }else{
                result += "\nisBusy: False";
            }
            
            if(this->activeOrderId >=0)
                result += "\nOrderId: " + std::to_string(activeOrderId);
            else
                result += "\nOrderId: None";
            if(this->getActiveOrderId() != -1)
                result += "\nDistance Left: " + std::to_string(this->getDistanceLeft());
            else
                result += "\nDistance Left: None";
            result += "\nOrdersLeft: " + std::to_string(this->getNumOrdersLeft());
            return result;
        }

        string LimitedDriverVolunteer::getCustType() const
        {
            return "LimitedDriver";
        }
        

