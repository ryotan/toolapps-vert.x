'use strict';

angular.module('toolappsClientApp')
  .controller('MainCtrl', function($scope, $http) {
    $scope.awesomeThings = [
      'HTML5 Boilerplate',
      'AngularJS',
      'Karma'
    ];
    $http.get('/digest?target=<script></script>').success(function(data) {
      $scope.raw = data.raw;
      $scope.digest = data.digest;
    });
    $scope.getDigest = function() {
      $http.get('/digest?target=' + $scope.raw).success(function(data) {
        $scope.digest = data.digest;
      });
    }
  });
